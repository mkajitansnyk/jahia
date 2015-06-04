/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.services.content.nodetypes;

import com.google.common.collect.Sets;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.templates.ModuleVersion;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.Patterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Jahia implementation of the {@link NodeTypeManager}.
 * User: toto
 * Date: 4 janv. 2008
 * Time: 15:08:56
 */
public class NodeTypeRegistry implements NodeTypeManager, InitializingBean {
    private static final String SYSTEM = "system";
    private static final Logger logger = LoggerFactory.getLogger(NodeTypeRegistry.class);

    private final Map<Name, ExtendedNodeType> nodetypes = new HashMap<>();

    private final BidiMap namespaces = new DualHashBidiMap();

    @SuppressWarnings("unchecked")
    private final Map<String, List<Resource>> files = new ListOrderedMap();

    private final Map<ExtendedNodeType, Set<ExtendedNodeType>> mixinExtensions = new HashMap<>();
    private final Map<String, Set<ExtendedItemDefinition>> typedItems = new HashMap<>();

    private boolean propertiesLoaded = false;
    private final Properties deploymentProperties = new Properties();

    private static boolean hasEncounteredIssuesWithDefinitions = false;
    private NodeTypesDBServiceImpl nodeTypesDBService;

    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    private Lock writeLock = readWriteLock.writeLock();

    // Initialization on demand holder idiom: thread-safe singleton initialization
    private static class Holder {
        static final NodeTypeRegistry INSTANCE = new NodeTypeRegistry();
    }
    public static NodeTypeRegistry getInstance() {
        return Holder.INSTANCE;
    }

    private static class ProviderNodeTypeRegistryHolder {
        static final NodeTypeRegistry PROVIDER_NODE_TYPE_INSTANCE = new NodeTypeRegistry();

        static {
            final NodeTypeRegistry instance = getInstance();
            try {
                PROVIDER_NODE_TYPE_INSTANCE.initSystemDefinitions();

                List<String> files = new ArrayList<>();
                List<String> remfiles;
                try {
                    remfiles = new ArrayList<>(instance.getNodeTypesDBService().getFilesList());
                    while (!remfiles.isEmpty() && !remfiles.equals(files)) {
                        files = new ArrayList<>(remfiles);
                        remfiles.clear();
                        for (String file : files) {
                            try {
                                if (file.endsWith(".cnd")) {
                                    final String cndFile = instance.getNodeTypesDBService().readCndFile(file);
                                    deployDefinitionsFileToProviderNodeTypeRegistry(new StringReader(cndFile), file);
                                }
                            } catch (ParseException e) {
                                remfiles.add(file);
                            }
                        }
                    }
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                }

            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        public static void deployDefinitionsFileToProviderNodeTypeRegistry(Reader reader, String definitionName) throws ParseException, IOException {
            final String systemId = StringUtils.substringBefore(definitionName, ".cnd");
            JahiaCndReader r = new JahiaCndReader(reader, definitionName, systemId, PROVIDER_NODE_TYPE_INSTANCE);
            r.parse();
        }

    }

    public static NodeTypeRegistry getProviderNodeTypeRegistry() {
        return ProviderNodeTypeRegistryHolder.PROVIDER_NODE_TYPE_INSTANCE;
    }

    public static void deployDefinitionsFileToProviderNodeTypeRegistry(Reader reader, String definitionName) throws ParseException, IOException {
        ProviderNodeTypeRegistryHolder.deployDefinitionsFileToProviderNodeTypeRegistry(reader, definitionName);
    }

    /**
     * Flush all labels for all node types and items
     */
    public void flushLabels() {
        writeLock.lock();
        for (ExtendedNodeType nodeType : nodetypes.values()) {
            nodeType.clearLabels();
        }
        for (Set<ExtendedItemDefinition> itemSet : typedItems.values()) {
            for (ExtendedItemDefinition item : itemSet) {
                item.clearLabels();
                item.getDeclaringNodeType().clearLabels();
            }
        }
        writeLock.unlock();
    }

    public void initSystemDefinitions() throws IOException {
        String cnddir = SettingsBean.getInstance().getJahiaEtcDiskPath() + "/repository/nodetypes";
        try {
            File f = new File(cnddir);
            SortedSet<File> cndfiles = new TreeSet<>(Arrays.asList(f.listFiles()));
            for (File file : cndfiles) {
                addDefinitionsFile(file, SYSTEM + "-" + Patterns.DASH.split(file.getName())[1], null);
            }
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void initPropertiesFile() throws IOException {
        try {
            final String propertyFile = nodeTypesDBService.readDefinitionPropertyFile();
            if (propertyFile != null) {
                deploymentProperties.load(new StringReader(propertyFile));
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        propertiesLoaded = true;
    }

    public void saveProperties() throws IOException {
        if (propertiesLoaded) {
            synchronized (deploymentProperties) {
                final StringWriter writer = new StringWriter();
                deploymentProperties.store(writer, "");
                try {
                    nodeTypesDBService.saveDefinitionPropertyFile(writer.toString());
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    public Properties getDeploymentProperties() {
        return deploymentProperties;
    }


    public boolean isLatestDefinitions(String systemId, ModuleVersion version) {
        if (version != null) {
            String key = systemId + ".version";
            if (deploymentProperties.containsKey(key)) {
                ModuleVersion lastDeployed = new ModuleVersion(deploymentProperties.getProperty(key));
                if (lastDeployed.compareTo(version) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public void addDefinitionsFile(Resource resource, String systemId, ModuleVersion version) throws IOException, ParseException {
        if (version != null) {
            if (isLatestDefinitions(systemId, version)) {
                deploymentProperties.put(systemId + ".version", version.toString());
                saveProperties();
            } else {
                return;
            }
        }

        String ext = resource.getURL().getPath().substring(resource.getURL().getPath().lastIndexOf('.'));
        if (ext.equalsIgnoreCase(".cnd")) {
            Reader resourceReader = null;
            try {
                resourceReader = new InputStreamReader(resource.getInputStream(), Charsets.UTF_8);
                JahiaCndReader r = new JahiaCndReader(resourceReader, resource.toString(), systemId, this);
                r.parse();
                if (r.hasEncounteredIssuesWithDefinitions()) {
                    hasEncounteredIssuesWithDefinitions = true;
                }
            } finally {
                IOUtils.closeQuietly(resourceReader);
            }
        } else if (ext.equalsIgnoreCase(".grp")) {
            Reader resourceReader = null;
            try {
                resourceReader = new InputStreamReader(resource.getInputStream(), Charsets.UTF_8);
                JahiaGroupingFileReader r = new JahiaGroupingFileReader(resourceReader, resource.toString(), systemId, this);
                r.parse();
            } finally {
                IOUtils.closeQuietly(resourceReader);
            }
        }

        if (!files.containsKey(systemId)) {
            files.put(systemId, new ArrayList<Resource>());
        }
        if (!files.get(systemId).contains(resource)) {
            files.get(systemId).add(resource);
        }
    }

    public void addDefinitionsFile(File file, String systemId, ModuleVersion version) throws ParseException, IOException {
        addDefinitionsFile(file == null ? null : new FileSystemResource(file), systemId, version);
    }

    /**
     * Reads the specified CND file resource and parses it to obtain a list of node type definitions.
     * 
     * @param resource
     *            a resource, representing a CND file
     * @param systemId
     *            the ID to use to specify the "origin" on the node types from this file
     * @return a list of the node types parsed from the specified resource
     * @throws ParseException
     *             in case of a parsing error
     * @throws IOException
     *             in case of an I/O error when reading the specified resource
     */
    public List<ExtendedNodeType> getDefinitionsFromFile(Resource resource, String systemId) throws ParseException, IOException {
        String ext = resource.getURL().getPath().substring(resource.getURL().getPath().lastIndexOf('.'));
        if (ext.equalsIgnoreCase(".cnd")) {
            Reader resourceReader = null;
            try {
                resourceReader = new InputStreamReader(resource.getInputStream(), Charsets.UTF_8);
                JahiaCndReader r = new JahiaCndReader(resourceReader, resource.getURL().getPath(), systemId, this);
                r.setDoRegister(false);
                r.parse();
                return r.getNodeTypesList();
            } finally {
                IOUtils.closeQuietly(resourceReader);
            }
        }
        return Collections.emptyList();
    }

    public void validateDefinitionsFile(InputStream inputStream, String filename, String systemId) throws ParseException, IOException, RepositoryException {
        if (filename.toLowerCase().endsWith(".cnd")) {
            Reader resourceReader = null;
            try {
                resourceReader = new InputStreamReader(inputStream, Charsets.UTF_8);
                JahiaCndReader r = new JahiaCndReader(resourceReader, filename, systemId, this);
                r.parse();

                if (r.hasEncounteredIssuesWithDefinitions()) {
                    throw new RepositoryException(StringUtils.join(r.getParsingErrors(), "\n"));
                }

                for (ExtendedNodeType nodeType : r.getNodeTypesList()) {
                    if (NodeTypeRegistry.getInstance().hasNodeType(nodeType.getName())) {
                        ExtendedNodeType existingNodeType = NodeTypeRegistry.getInstance().getNodeType(nodeType.getName());
                        if (!existingNodeType.getSystemId().equals(nodeType.getSystemId())) {
                            throw new NodeTypeExistsException("Node type already defined : " + nodeType.getName());
                        }
                    }
                }
            } finally {
                IOUtils.closeQuietly(resourceReader);
            }
        }
    }

    public List<String> getSystemIds() {
        return new ArrayList<>(files.keySet());
    }

    public List<Resource> getFiles(String systemId) {
        return files.get(systemId);
    }

    public ExtendedNodeType getNodeType(String name) throws NoSuchNodeTypeException {
        readLock.lock();
        ExtendedNodeType res = StringUtils.isNotEmpty(name) ? nodetypes.get(new Name(name, namespaces)) : null;
        readLock.unlock();
        if (res == null) {
            throw new NoSuchNodeTypeException("Unknown type : " + name);
        }
        return res;
    }

    public JahiaNodeTypeIterator getAllNodeTypes() {
        readLock.lock();
        final Collection<ExtendedNodeType> values = nodetypes.values();
        readLock.unlock();
        return new JahiaNodeTypeIterator(values.iterator(), values.size());
    }

    public JahiaNodeTypeIterator getAllNodeTypes(List<String> systemIds) {
        if (systemIds == null || systemIds.isEmpty()) {
            return getAllNodeTypes();
        } else {
            List<ExtendedNodeType> res = new ArrayList<>();
            readLock.lock();
            for (ExtendedNodeType nt : nodetypes.values()) {
                if (systemIds.contains(nt.getSystemId())) {
                    res.add(nt);
                }
            }
            readLock.unlock();
            return new JahiaNodeTypeIterator(res.iterator(), res.size());
        }

    }

    public JahiaNodeTypeIterator getNodeTypes(String systemId) {
        return getAllNodeTypes(Collections.singletonList(systemId));
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getNamespaces() {
        return namespaces;
    }
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        return getPrimaryNodeTypes(null);
    }

    public NodeTypeIterator getPrimaryNodeTypes(List<String> systemIds) throws RepositoryException {
        final boolean addAll = systemIds == null || systemIds.isEmpty();

        List<ExtendedNodeType> res = new ArrayList<>();
        readLock.unlock();
        for (ExtendedNodeType nt : nodetypes.values()) {
            if (!nt.isMixin() && (addAll || systemIds.contains(nt.getSystemId()))) {
                res.add(nt);
            }
        }
        readLock.unlock();
        return new JahiaNodeTypeIterator(res.iterator(), res.size());
    }

    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        return getMixinNodeTypes(null);
    }

    public NodeTypeIterator getMixinNodeTypes(List<String> systemIds) throws RepositoryException {
        final boolean addAll = systemIds == null || systemIds.isEmpty();

        List<ExtendedNodeType> res = new ArrayList<>();
        readLock.lock();
        for (ExtendedNodeType nt : nodetypes.values()) {
            if (nt.isMixin() && (addAll || systemIds.contains(nt.getSystemId()))) {
                res.add(nt);
            }
        }
        readLock.unlock();
        return new JahiaNodeTypeIterator(res.iterator(), res.size());
    }

    public void addNodeType(Name name, ExtendedNodeType nodeType) throws NodeTypeExistsException {
        readLock.lock();
        if (nodetypes.containsKey(name)) {
            final String systemId = nodetypes.get(name).getSystemId();
            if (!systemId.equals(nodeType.getSystemId())) {
                readLock.unlock();
                throw new NodeTypeExistsException("Node type '" + name + "' already defined with a different systemId (existing: '"
                        + systemId + "', provided: '" + nodeType.getSystemId() + "' with name: '" + nodeType.getName() + "')");
            }
        }
        readLock.unlock();
        writeLock.lock();
        nodetypes.put(name, nodeType);
        writeLock.unlock();
    }

    public void addMixinExtension(ExtendedNodeType mixin, ExtendedNodeType baseType) {
        readLock.lock();
        if (!mixinExtensions.containsKey(baseType)) {
            readLock.unlock();
            writeLock.lock();
            mixinExtensions.put(baseType, new HashSet<ExtendedNodeType>());
            writeLock.unlock();
            readLock.lock();
        }
        readLock.unlock();
        writeLock.lock();
        mixinExtensions.get(baseType).remove(mixin);
        mixinExtensions.get(baseType).add(mixin);
        writeLock.unlock();

    }

    public Map<ExtendedNodeType, Set<ExtendedNodeType>> getMixinExtensions() {
        return mixinExtensions;
    }

    public void addTypedItem(ExtendedItemDefinition itemDef) {
        final String type = itemDef.getItemType();
        readLock.lock();
        if (!typedItems.containsKey(type)) {
            readLock.unlock();
            writeLock.lock();
            typedItems.put(type, new HashSet<ExtendedItemDefinition>());
            writeLock.unlock();
            readLock.lock();
        }
        readLock.unlock();
        writeLock.lock();
        typedItems.get(type).add(itemDef);
        writeLock.unlock();
    }

    public Map<String, Set<ExtendedItemDefinition>> getTypedItems() {
        return typedItems;
    }

    public void unregisterNodeType(Name name) {
        writeLock.lock();
        nodetypes.remove(name);
        writeLock.unlock();
    }

    public void unregisterNodeTypes(String systemId) {
        readLock.lock();
        for (Name n : new HashSet<>(nodetypes.keySet())) {
            ExtendedNodeType nt = nodetypes.get(n);
            if (systemId.equals(nt.getSystemId())) {
                readLock.unlock();
                unregisterNodeType(n);
                readLock.lock();
            }
        }
        readLock.unlock();
        deploymentProperties.remove(systemId + ".version");
        try {
            saveProperties();
        } catch (IOException e) {
            logger.error("Cannot save definitions properties", e);
        }
    }

    public void setNodeTypesDBService(NodeTypesDBServiceImpl nodeTypesDBService) {
        this.nodeTypesDBService = nodeTypesDBService;
    }

    public NodeTypesDBServiceImpl getNodeTypesDBService() {
        return nodeTypesDBService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            initPropertiesFile();
            initSystemDefinitions();
        } catch (IOException e) {
            logger.error("Cannot load definition deployment properties");
        }
    }

    public class JahiaNodeTypeIterator implements NodeTypeIterator, Iterable<ExtendedNodeType> {
        private long size;
        private long pos = 0;
        private final Iterator<ExtendedNodeType> iterator;

        JahiaNodeTypeIterator(Iterator<ExtendedNodeType> it, long size) {
            this.iterator = it;
            this.size = size;
        }

        public NodeType nextNodeType() {
            pos += 1;
            return iterator.next();
        }

        public void skip(long l) {
            if ((pos + l + 1) > size) {
                throw new NoSuchElementException("Tried to skip past " + l +
                        " elements, which with current pos (" + pos +
                        ") brings us past total size=" + size);
            }
            for (int i = 0; i < l; i++) {
                next();
            }
        }

        public long getSize() {
            return size;
        }

        public long getPosition() {
            return pos;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Object next() {
            pos += 1;
            return iterator.next();
        }

        public void remove() {
            iterator.remove();
            size -= 1;
        }

        /**
         * Returns an iterator over a set of elements of type T.
         *
         * @return an Iterator.
         */
        @Override
        public Iterator<ExtendedNodeType> iterator() {
            return this;
        }
    }

    public boolean hasNodeType(String name) {
        return nodetypes.get(new Name(name, namespaces)) != null;
    }

    public NodeTypeTemplate createNodeTypeTemplate() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public NodeDefinitionTemplate createNodeDefinitionTemplate() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public PropertyDefinitionTemplate createPropertyDefinitionTemplate() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public NodeTypeIterator registerNodeTypes(NodeTypeDefinition[] ntds, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public void unregisterNodeType(String name) throws ConstraintViolationException {
        Name n = new Name(name, namespaces);
        readLock.lock();
        if (nodetypes.containsKey(n)) {
            for (ExtendedNodeType type : nodetypes.values()) {
                if (!type.getName().equals(name)) {
                    for (ExtendedNodeType nt : type.getSupertypes()) {
                        if (nt.getName().equals(name)) {
                            readLock.unlock();
                            throw new ConstraintViolationException("Cannot unregister node type " + name + " because " + type.getName() + " extends it.");
                        }
                    }
                    for (ExtendedNodeDefinition ntd : type.getChildNodeDefinitions()) {
                        if (Sets.newHashSet(ntd.getRequiredPrimaryTypeNames()).contains(name)) {
                            readLock.unlock();
                            throw new ConstraintViolationException("Cannot unregister node type " + name + " because a child node definition of " + type.getName() + " requires it.");
                        }
                    }
                    for (ExtendedNodeDefinition ntd : type.getUnstructuredChildNodeDefinitions().values()) {
                        if (Sets.newHashSet(ntd.getRequiredPrimaryTypeNames()).contains(name)) {
                            readLock.unlock();
                            throw new ConstraintViolationException("Cannot unregister node type " + name + " because a child node definition of " + type.getName() + " requires it.");
                        }
                    }
                }
            }
            readLock.unlock();
            writeLock.lock();
            nodetypes.remove(n);
            writeLock.unlock();
        }
    }

    public void unregisterNodeTypes(String[] names) throws ConstraintViolationException {
        for (String name : names) {
            unregisterNodeType(name);
        }
    }

    /**
     * Indicates if any issue related to the definitions has been encountered since the last startup. When this method
     * returns true, the only way to get back false as a return value is to restart Jahia.
     *
     * @return true if an issue with the def has been encountered, false otherwise.
     * @since 6.6.2.0
     */
    public final boolean hasEncounteredIssuesWithDefinitions() {
        return hasEncounteredIssuesWithDefinitions;
    }
}

