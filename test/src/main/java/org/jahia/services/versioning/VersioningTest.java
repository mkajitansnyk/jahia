package org.jahia.services.versioning;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.jahia.api.Constants;
import org.jahia.bin.Jahia;
import org.jahia.params.ProcessingContext;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPublicationService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.VersionInfo;
import org.jahia.services.sites.JahiaSite;
import org.jahia.test.TestHelper;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import java.util.List;

/**
 * Unit test to test version listing created during publication
 */
public class VersioningTest extends TestCase {
    private static Logger logger = Logger.getLogger(VersioningTest.class);
    private JahiaSite site;
    private ProcessingContext ctx;
    private final static String TESTSITE_NAME = "jcrVersioningTest";
    private final static String SITECONTENT_ROOT_NODE = "/sites/" + TESTSITE_NAME;

    protected void setUp() throws Exception {
        try {
            site = TestHelper.createSite(TESTSITE_NAME);
            ctx = Jahia.getThreadParamBean();
            assertNotNull(site);
        } catch (Exception ex) {
            logger.warn("Exception during test setUp", ex);
        }
    }


    /**
     * Test number of version after publication
     *
     * @throws Exception
     */
    public void testVersions() throws Exception {
        try {
            JCRPublicationService jcrService = ServicesRegistry.getInstance().getJCRPublicationService();

            JCRSessionWrapper editSession = jcrService.getSessionFactory().getCurrentUserSession();
            JCRSessionWrapper liveSession = jcrService.getSessionFactory().getCurrentUserSession(Constants.LIVE_WORKSPACE);


            JCRNodeWrapper stageRootNode = editSession.getNode(SITECONTENT_ROOT_NODE);

            Node versioningTestActivity = editSession.getWorkspace().getVersionManager().createActivity("versioningTest");
            Node previousActivity = editSession.getWorkspace().getVersionManager().setActivity(versioningTestActivity);
            if (previousActivity != null) {
                logger.debug("Previous activity=" + previousActivity.getName() + " new activity=" + versioningTestActivity.getName());
            } else {
                logger.debug("New activity=" + versioningTestActivity.getName());
            }

            // get home page
            JCRNodeWrapper stageNode = stageRootNode.getNode("home");

            editSession.checkout(stageNode);
            JCRNodeWrapper stagedSubPage = stageNode.addNode("home_subpage1", "jnt:page");
            stagedSubPage.setProperty("jcr:title", "title0");
            editSession.save();

            // publish it
            jcrService.publish(stageNode.getPath(), Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE, null, false, true);

            for (int i = 1; i < 11; i++) {
                editSession.checkout(stagedSubPage);
                stagedSubPage.setProperty("jcr:title", "title" + i);
                editSession.save();

                // each time the node i published, a new version should be created
                jcrService.publish(stagedSubPage.getPath(), Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE, null, false, false);
            }

            // now let's do a little system versioning ourselves...

            editSession.getWorkspace().getVersionManager().checkpoint(stagedSubPage.getPath());

            // let's do some validation checks, first for the live workspace...

            // check number of versions
            JCRNodeWrapper subPagePublishedNode = liveSession.getNode(stagedSubPage.getPath());

            List<VersionInfo> liveVersionInfos = ServicesRegistry.getInstance().getJCRVersionService().getVersionInfos(liveSession, subPagePublishedNode);
            int index = 0;
            for (VersionInfo curVersionInfo : liveVersionInfos) {
                String versionName = curVersionInfo.getVersion().getName();
                JCRNodeWrapper versionNode = subPagePublishedNode.getFrozenVersionAsRegular(curVersionInfo.getVersion().getCreated().getTime());
                validateVersionedNode(index, curVersionInfo, versionName, versionNode);
                index++;
            }
            logger.debug("number of version: " + index);
            assertEquals(11, index);

            List<VersionInfo> editVersionInfos = ServicesRegistry.getInstance().getJCRVersionService().getVersionInfos(editSession, stageNode);
            index = 0;
            for (VersionInfo curVersionInfo : editVersionInfos) {
                String versionName = curVersionInfo.getVersion().getName();
                JCRNodeWrapper versionNode = stagedSubPage.getFrozenVersionAsRegular(curVersionInfo.getVersion().getCreated().getTime());
                validateVersionedNode(index, curVersionInfo, versionName, versionNode);
                index++;
            }
            logger.debug("number of version: " + index);
            assertEquals(11, index);

        } catch (Exception ex) {
            logger.warn("Exception during test", ex);
            throw ex;
        }
    }

    private void validateVersionedNode(int index, VersionInfo curVersionInfo, String versionName, JCRNodeWrapper versionNode) throws RepositoryException {
        String versionTitle = versionNode.getPropertyAsString("jcr:title");
        String title = "title" + index;
        logger.debug("version number:"+versionName +", jcr:title: " + versionTitle + " created=" + curVersionInfo.getVersion().getCreated().getTime() + " revisionNumber=" + Long.toString(curVersionInfo.getRevisionNumber()));
        assertEquals(title, versionTitle);
        // let's check the version node's path
        assertEquals("Versioned node path is invalid !", SITECONTENT_ROOT_NODE + "/home/home_subpage1", versionNode.getPath());
        // let's check the node type
        assertEquals("Versioned node should be viewed as a node type jnt:page", "jnt:page", versionNode.getPrimaryNodeTypeName());
        // let's check the mixin types
        assertTrue("Versioned node should be viewed as a mixin node type jmix:basemetadata", versionNode.isNodeType("jmix:basemetadata"));
        assertTrue("Versioned node should be viewed as a mixin node type jmix:hierarchyNode", versionNode.isNodeType("jmix:hierarchyNode"));
        assertTrue("Versioned node should be viewed as a mixin node type jmix:renderable", versionNode.isNodeType("jmix:renderable"));

        // now let's check the parent
        JCRNodeWrapper parentVersionNode = versionNode.getParent();
        assertEquals("Parent node name is not correct", "home", parentVersionNode.getName());
        assertEquals("Parent node type is not of type jnt:page", "jnt:page", parentVersionNode.getPrimaryNodeTypeName());
        assertEquals("Parent node path invalid", SITECONTENT_ROOT_NODE + "/home", parentVersionNode.getPath());

        // now let's check the child objects.

    }


    @Override
    protected void tearDown() throws Exception {
        try {
            TestHelper.deleteSite(TESTSITE_NAME);
        } catch (Exception ex) {
            logger.warn("Exception during test tearDown", ex);
        }
    }

}
