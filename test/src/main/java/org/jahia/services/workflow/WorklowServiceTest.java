/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Limited. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have recieved a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license"
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.services.workflow;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.test.TestHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

/**
 * Unit test for the {@link WorkflowService}.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 2 févr. 2010
 */
public class WorklowServiceTest {
    private final static String TESTSITE_NAME = "jBPMWorkflowServiceTest";
    private final static String SITECONTENT_ROOT_NODE = "/sites/" + TESTSITE_NAME + "/home/pagecontent/row1/col1";
    private static JahiaSite site;
    private static JahiaUser johndoe;
    private static JahiaUser johnsmoe;
    private static JahiaGroup group;
    private HashMap<String, Object> emptyMap;
    private static final String PROVIDER = "jBPM";
    private static Logger logger = Logger.getLogger(WorklowServiceTest.class);
    private JCRSessionWrapper session;
    private static int nodeCounter;
    private JCRNodeWrapper stageNode;
    private static WorkflowService service;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        site = TestHelper.createSite(TESTSITE_NAME);
        assertNotNull("Unable to create test site", site);
        initUsersGroup();
        JCRSessionFactory.getInstance().getCurrentUserSession().save();
        service = WorkflowService.getInstance();
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        try {
            TestHelper.deleteSite(TESTSITE_NAME);
        } catch (Exception ex) {
            logger.warn("Exception during test tearDown", ex);
        }
        JahiaUserManagerService userManagerService = ServicesRegistry.getInstance().getJahiaUserManagerService();
        JahiaGroupManagerService groupManagerService = ServicesRegistry.getInstance().getJahiaGroupManagerService();
        groupManagerService.deleteGroup(groupManagerService.lookupGroup(site.getID(), "taskUsersGroup"));
        userManagerService.deleteUser(userManagerService.lookupUser("johndoe"));
        userManagerService.deleteUser(userManagerService.lookupUser("johnsmoe"));
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
        session.save();
        session.logout();
    }
    
    @Before
    public void setUp() throws Exception {
        session = JCRSessionFactory.getInstance().getCurrentUserSession(null, Locale.ENGLISH);
        JCRNodeWrapper root = session.getNode(SITECONTENT_ROOT_NODE);
        session.checkout(root);
        stageNode = root.addNode("child-" + ++nodeCounter, "jnt:text");
        session.save();
        emptyMap = new HashMap<String, Object>();
    }

    @Test
    public void testGetPossibleWorkflow() throws Exception {
        final List<WorkflowDefinition> workflowList =  WorkflowService.getInstance().getPossibleWorkflows(stageNode, null);
        assertTrue("There should be some workflows already deployed", workflowList.size() > 0);
    }

    @Test
    public void testGetActiveWorkflows() throws Exception {
        final List<WorkflowDefinition> workflowList = service.getPossibleWorkflows(stageNode, null);
        assertTrue("There should be some workflows already deployed", workflowList.size() > 0);
        final WorkflowDefinition workflow = workflowList.get(0);
        assertNotNull("Worflow should not be null", workflow);
        final String processId = service.startProcess(stageNode, workflow.getKey(), "jBPM",
                                                      new HashMap<String, Object>());
        assertNotNull("The startup of a process should have return an id", processId);
        final List<Workflow> activeWorkflows = service.getActiveWorkflows(stageNode);
        assertTrue("There should be some active workflow in jBPM", activeWorkflows.size() > 0);
        assertTrue("There should be some active activities for the first workflow in jBPM", activeWorkflows.get(0)
                .getAvailableActions().size() > 0);
    }

    @Test
    public void testSignalProcess() throws Exception {
        final List<WorkflowDefinition> workflowList = service.getPossibleWorkflows(stageNode, null);
        assertTrue("There should be some workflows already deployed", workflowList.size() > 0);
        final WorkflowDefinition workflow = workflowList.get(0);
        assertNotNull("Workflow should not be null", workflow);
        final String processId = service.startProcess(stageNode, workflow.getKey(), "jBPM", emptyMap);
        assertNotNull("The startup of a process should have return an id", processId);
        final List<Workflow> activeWorkflows = service.getActiveWorkflows(stageNode);
        assertTrue("There should be some active workflow in jBPM", activeWorkflows.size() > 0);
        final Set<WorkflowAction> availableActions = activeWorkflows.get(0).getAvailableActions();
        assertTrue("There should be some active activities for the first workflow in jBPM",
                   availableActions.size() > 0);
        WorkflowAction action = availableActions.iterator().next();
        if (action instanceof WorkflowTask) {
            service.signalProcess(processId, action.getName(), ((WorkflowTask) action).getOutcomes().contains(
                    "accept") ? "accept" : "reject", "jBPM", emptyMap);
        } else {
            service.signalProcess(processId, action.getName(), "jBPM", emptyMap);
        }
        final List<Workflow> newActiveWorkflows = service.getActiveWorkflows(stageNode);
        assertTrue("There should be some active workflow in jBPM", newActiveWorkflows.size() > 0);
        final Set<WorkflowAction> newAvailableActions = newActiveWorkflows.get(0).getAvailableActions();
        assertTrue("There should be some active activities for the first workflow in jBPM",
                   availableActions.size() > 0);
        assertFalse("Available actions should not match", availableActions.equals(newAvailableActions));
        assertTrue("Available action should match between service.getActiveWorkflows and getAvailableActions",
                   newAvailableActions.equals(service.getAvailableActions(processId, "jBPM")));
    }

    @Test
    public void testAssignTask() throws Exception {
        final List<WorkflowDefinition> workflowList = service.getPossibleWorkflows(stageNode, null);
        assertTrue("There should be some workflows already deployed", workflowList.size() > 0);
        final WorkflowDefinition workflow = workflowList.get(0);
        assertNotNull("Workflow should not be null", workflow);
        final String processId = service.startProcess(stageNode, workflow.getKey(), "jBPM",
                                                      new HashMap<String, Object>());
        assertNotNull("The startup of a process should have return an id", processId);
        final List<Workflow> activeWorkflows = service.getActiveWorkflows(stageNode);
        assertTrue("There should be some active workflow in jBPM", activeWorkflows.size() > 0);
        Set<WorkflowAction> actionSet = activeWorkflows.get(0).getAvailableActions();
        assertTrue("There should be some active activities for the first workflow in jBPM", actionSet.size() > 0);
        WorkflowAction action = actionSet.iterator().next();
        assertTrue(action instanceof WorkflowTask);
        WorkflowTask task = (WorkflowTask) action;
        JahiaUser user = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupUser("root");
        assertNotNull(user);
        service.assignTask(task.getId(), "jBPM", user);
        List<WorkflowTask> forUser = service.getTasksForUser(user);
        assertTrue(forUser.size() > 0);
        final HashMap<String, Object> emptyMap = new HashMap<String, Object>();
        WorkflowTask workflowTask = forUser.get(0);
        service.completeTask(workflowTask.getId(), "jBPM", workflowTask.getOutcomes().contains(
                "accept") ? "accept" : "reject", emptyMap);
        assertTrue(service.getTasksForUser(user).size() < forUser.size());
        assertFalse(service.getActiveWorkflows(stageNode).equals(actionSet));
    }

    @Test
    public void testAddParticipatingGroup() throws Exception {
        final List<WorkflowDefinition> workflowList = service.getPossibleWorkflows(stageNode, null);
        assertTrue("There should be some workflows already deployed", workflowList.size() > 0);
        final WorkflowDefinition workflow = workflowList.get(0);
        assertNotNull("Workflow should not be null", workflow);
        final String processId = service.startProcess(stageNode, workflow.getKey(), PROVIDER,
                                                      new HashMap<String, Object>());
        assertNotNull("The startup of a process should have return an id", processId);
        final List<Workflow> activeWorkflows = service.getActiveWorkflows(stageNode);
        assertTrue("There should be some active workflow in jBPM", activeWorkflows.size() > 0);
        Set<WorkflowAction> actionSet = activeWorkflows.get(0).getAvailableActions();
        assertTrue("There should be some active activities for the first workflow in jBPM", actionSet.size() > 0);
        WorkflowAction action = actionSet.iterator().next();
        assertTrue(action instanceof WorkflowTask);
        WorkflowTask task = (WorkflowTask) action;
        service.addParticipatingGroup(task.getId(), PROVIDER, group, WorkflowService.CANDIDATE);
        List<WorkflowTask> johnDoeList = service.getTasksForUser(johndoe);
        List<WorkflowTask> johnSmoeList = service.getTasksForUser(johnsmoe);
        assertTrue("John Doe and John Smoe should have the same tasks list", johnDoeList.equals(johnSmoeList));
        service.assignTask(johnDoeList.get(0).getId(), PROVIDER, johndoe);
        johnSmoeList = service.getTasksForUser(johnsmoe);
        johnDoeList = service.getTasksForUser(johndoe);
        assertFalse("John Doe and John Smoe should not have same tasks list", johnDoeList.equals(johnSmoeList));
        service.completeTask(task.getId(), PROVIDER, task.getOutcomes().iterator().next(),
                             new HashMap<String, Object>());
    }

    @Test
    public void testFullProcess2StepPublication() throws Exception {
        final List<WorkflowDefinition> workflowList = service.getPossibleWorkflows(stageNode, null);
        assertTrue("There should be some workflows already deployed", workflowList.size() > 0);
        WorkflowDefinition workflow = null;
        for (WorkflowDefinition workflowDefinition : workflowList) {
            if ("2 Step Publication Process".equals(workflowDefinition.getName())) {
                workflow = workflowDefinition;
                break;
            }
        }
        assertNotNull("Unable to find workflow process '2 Step Publication Process'", workflow);
        final String processId = service.startProcess(stageNode, workflow.getKey(), PROVIDER,
                                                      new HashMap<String, Object>());
        assertNotNull("The startup of a process should have return an id", processId);
        final List<Workflow> activeWorkflows = service.getActiveWorkflows(stageNode);
        assertTrue("There should be some active workflow in jBPM", activeWorkflows.size() > 0);
        Set<WorkflowAction> actionSet = activeWorkflows.get(0).getAvailableActions();
        assertTrue("There should be some active activities for the first workflow in jBPM", actionSet.size() > 0);
        WorkflowAction action = actionSet.iterator().next();
        assertTrue(action instanceof WorkflowTask);
        WorkflowTask task = (WorkflowTask) action;
        service.assignTask(task.getId(), PROVIDER, johndoe);
        List<WorkflowTask> forUser = service.getTasksForUser(johndoe);
        assertTrue(forUser.size() > 0);
        WorkflowTask workflowTask = forUser.get(0);
        service.completeTask(workflowTask.getId(), PROVIDER, "accept", emptyMap);
        assertTrue(service.getTasksForUser(johndoe).size() < forUser.size());
        assertFalse(service.getActiveWorkflows(stageNode).equals(actionSet));
        // Assign john smoe to the next task
        actionSet = service.getAvailableActions(processId, PROVIDER);
        service.assignTask(((WorkflowTask)actionSet.iterator().next()).getId(), PROVIDER, johnsmoe);
        // Rollback to previous task
        forUser = service.getTasksForUser(johnsmoe);
        assertTrue("John Smoe task list should not be empty", forUser.size() > 0);
        assertTrue("Current task should be final review", forUser.get(0).getName().equals("final review"));
        workflowTask = forUser.get(0);
        assertTrue("Final review should have 3 outcomes", workflowTask.getOutcomes().size() == 3);
        assertTrue("Final review should contains correction needed as an outcome", workflowTask.getOutcomes().contains(
                "correction needed"));
        service.completeTask(workflowTask.getId(), workflowTask.getProvider(), "correction needed", emptyMap);
        assertTrue("Current Task should be first review as we have asked for corrections", service.getAvailableActions(
                processId, PROVIDER).iterator().next().getName().equals("first review"));
        // Assign john doe to task
        service.assignTask(((WorkflowTask)service.getAvailableActions(processId, PROVIDER).iterator().next()).getId(),
                           PROVIDER, johndoe);
        // Complete task
        service.completeTask(service.getTasksForUser(johndoe).get(0).getId(), PROVIDER, "accept", emptyMap);
        // Assign john smoe to the next task
        service.assignTask(((WorkflowTask)service.getAvailableActions(processId, PROVIDER).iterator().next()).getId(),
                           PROVIDER, johnsmoe);
        // Complete Task with accept
        service.completeTask(service.getTasksForUser(johnsmoe).get(0).getId(), PROVIDER, "publish", emptyMap);
        // Verify we are at publish state
        assertTrue("Current Task should be first review as we have accepted it",
                   service.getAvailableActions(processId, PROVIDER).iterator().next().getName().equals("publication"));
    }

    @After
    public void tearDown() throws Exception {
    }

    private static void initUsersGroup() {
        JahiaUserManagerService userManagerService = ServicesRegistry.getInstance().getJahiaUserManagerService();
        JahiaGroupManagerService groupManagerService = ServicesRegistry.getInstance().getJahiaGroupManagerService();
        johndoe = userManagerService.lookupUser("johndoe");
        johnsmoe = userManagerService.lookupUser("johnsmoe");
        Properties properties = new Properties();
        if (johndoe == null) {
            properties.setProperty("j:firstName", "John");
            properties.setProperty("j:lastName", "Doe");
            properties.setProperty("j:email", "johndoe@localhost.com");
            johndoe = userManagerService.createUser("johndoe", "johndoe", properties);
        }
        if (johnsmoe == null) {
            properties = new Properties();
            properties.setProperty("j:firstName", "John");
            properties.setProperty("j:lastName", "Smoe");
            properties.setProperty("j:email", "johnsmoe@localhost.com");
            johnsmoe = userManagerService.createUser("johnsmoe", "johnsmoe", properties);
        }
        group = groupManagerService.createGroup(site.getID(), "taskUsersGroup", null, true);
        group.addMember(johndoe);
        group.addMember(johnsmoe);
    }

    @Test
    public void testFullProcess1StepPublicationAccept() throws Exception {
        final List<WorkflowDefinition> workflowList = service.getPossibleWorkflows(stageNode, null);
        assertTrue("There should be some workflows already deployed", workflowList.size() > 0);
        WorkflowDefinition workflow = null;
        for (WorkflowDefinition workflowDefinition : workflowList) {
            if ("1 Step Publication Process".equals(workflowDefinition.getName())) {
                workflow = workflowDefinition;
                break;
            }
        }
        assertNotNull("Unable to find workflow process '1 Step Publication Process'", workflow);
        final String processId = service.startProcess(stageNode, workflow.getKey(), PROVIDER,
                                                      new HashMap<String, Object>());
        assertNotNull("The startup of a process should have return an id", processId);
        final List<Workflow> activeWorkflows = service.getActiveWorkflows(stageNode);
        assertTrue("There should be some active workflow in jBPM", activeWorkflows.size() > 0);
        Set<WorkflowAction> actionSet = activeWorkflows.get(0).getAvailableActions();
        assertTrue("There should be some active activities for the first workflow in jBPM", actionSet.size() > 0);
        WorkflowAction action = actionSet.iterator().next();
        assertTrue(action instanceof WorkflowTask);
        WorkflowTask task = (WorkflowTask) action;
        service.assignTask(task.getId(), PROVIDER, johndoe);
        List<WorkflowTask> forUser = service.getTasksForUser(johndoe);
        assertTrue(forUser.size() > 0);
        WorkflowTask workflowTask = forUser.get(0);
        service.completeTask(workflowTask.getId(), PROVIDER, "accept", emptyMap);
        assertTrue(service.getTasksForUser(johndoe).size() < forUser.size());
        assertTrue("The workflow process is not completed", service.getActiveWorkflows(stageNode).isEmpty());
    }

    @Test
    public void testFullProcess1StepPublicationReject() throws Exception {
        final List<WorkflowDefinition> workflowList = service.getPossibleWorkflows(stageNode, null);
        assertTrue("There should be some workflows already deployed", workflowList.size() > 0);
        WorkflowDefinition workflow = null;
        for (WorkflowDefinition workflowDefinition : workflowList) {
            if ("1 Step Publication Process".equals(workflowDefinition.getName())) {
                workflow = workflowDefinition;
                break;
            }
        }
        assertNotNull("Unable to find workflow process '1 Step Publication Process'", workflow);
        final String processId = service.startProcess(stageNode, workflow.getKey(), PROVIDER,
                                                      new HashMap<String, Object>());
        assertNotNull("The startup of a process should have return an id", processId);
        final List<Workflow> activeWorkflows = service.getActiveWorkflows(stageNode);
        assertTrue("There should be some active workflow in jBPM", activeWorkflows.size() > 0);
        Set<WorkflowAction> actionSet = activeWorkflows.get(0).getAvailableActions();
        assertTrue("There should be some active activities for the first workflow in jBPM", actionSet.size() > 0);
        WorkflowAction action = actionSet.iterator().next();
        assertTrue(action instanceof WorkflowTask);
        WorkflowTask task = (WorkflowTask) action;
        service.assignTask(task.getId(), PROVIDER, johndoe);
        List<WorkflowTask> forUser = service.getTasksForUser(johndoe);
        assertTrue(forUser.size() > 0);
        WorkflowTask workflowTask = forUser.get(0);
        service.completeTask(workflowTask.getId(), PROVIDER, "reject", emptyMap);
        assertTrue(service.getTasksForUser(johndoe).size() < forUser.size());
        assertTrue("The workflow process is not completed", service.getActiveWorkflows(stageNode).isEmpty());
    }

    @Test
    public void testHistory() throws Exception {
        testFullProcess1StepPublicationAccept();
        testFullProcess1StepPublicationReject();
        List<HistoryWorkflow> history = service.getHistoryWorkflows(stageNode);
        assertEquals("Node should have two history records", 2, history.size());
    }

}
