package org.jahia.services.content;

import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;

import javax.jcr.*;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.*;

/**
 * Jahia's wrapper of the JCR <code>javax.jcr.PropertyIterator</code>.
 *
 * @author toto
 */
public class LazyPropertyIterator implements PropertyIterator, Map {
    private JCRNodeWrapper node;
    private Locale locale;
    private String pattern;
    private PropertyIterator propertyIterator;
    private PropertyIterator i18nPropertyIterator;
    private Property tempNext = null;
    private String fallbackLocale;

    public LazyPropertyIterator(JCRNodeWrapper node) {
        this.node = node;
    }

    public LazyPropertyIterator(JCRNodeWrapper node, Locale locale) {
        this.node = node;
        this.locale = locale;
    }

    public LazyPropertyIterator(JCRNodeWrapper node, Locale locale, String pattern) {
        this.node = node;
        this.locale = locale;
        this.pattern = pattern;
    }

    public int size() {
        return (int) (getPropertiesIterator().getSize() + getI18NPropertyIterator().getSize());
    }

    private PropertyIterator getPropertiesIterator() {
        if (propertyIterator == null) {
            try {
                if (pattern == null) {
                    propertyIterator = node.getRealNode().getProperties();
                } else {
                    propertyIterator = node.getRealNode().getProperties(pattern);
                }
            } catch (RepositoryException e) {
                throw new RuntimeException("getI18NPropertyIterator",e);
            }
        }
        return propertyIterator;
    }

    private PropertyIterator getI18NPropertyIterator() {
        if (i18nPropertyIterator == null) {
            try {
                if (locale != null) {
                    final Node localizedNode = node.getI18N(locale);
                    fallbackLocale = localizedNode.getProperty("jcr:language").getString();
                    if (pattern == null) {
                        i18nPropertyIterator = localizedNode.getProperties();
                    } else {
                        i18nPropertyIterator = localizedNode.getProperties(pattern);
                    }
                } else {
                    i18nPropertyIterator = new EmptyPropertyIterator();
                }
            } catch (ItemNotFoundException e) {
                i18nPropertyIterator = new EmptyPropertyIterator();
            } catch (RepositoryException e) {
                throw new RuntimeException("getI18NPropertyIterator",e);
            }
        }
        return i18nPropertyIterator;
    }

    public boolean isEmpty() {
        return getPropertiesIterator().getSize() == 0 && getI18NPropertyIterator().getSize() == 0;
    }

    public Property nextProperty() {
        try {
            if (tempNext != null) {
                Property res = tempNext;
                tempNext = null;
                return res;
            }

            if (getPropertiesIterator().hasNext()) {
                Property property = getPropertiesIterator().nextProperty();
                ExtendedPropertyDefinition epd = node.getApplicablePropertyDefinition(property.getName());
                return new JCRPropertyWrapperImpl(node, property, node.getSession(), node.getProvider(), epd);
            } else {
                do {
                    Property property = getI18NPropertyIterator().nextProperty();
                    final String name = property.getName();
                    try {
                        final ExtendedPropertyDefinition def = node.getApplicablePropertyDefinition(name);
                        if (def.isInternationalized()) {
                            return new JCRPropertyWrapperImpl(node, property, node.getSession(), node.getProvider(), def, name);
                        }
                    } catch (ConstraintViolationException e) {
                    }
                } while (true);
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("nextProperty",e);
        }
    }

    public void skip(long skipNum) {
        for (int i=0; i < skipNum; i++) {
            if (getPropertiesIterator().hasNext()) {
                getPropertiesIterator().skip(1);
            } else {
                getI18NPropertyIterator().skip(1);
            }
        }
    }

    public long getSize() {
        return size();
    }

    public long getPosition() {
        return getPropertiesIterator().getPosition() + getI18NPropertyIterator().getPosition();
    }

    public boolean hasNext() {
        if (tempNext != null) {
            return true;
        }
        try {
            if (getPropertiesIterator().hasNext()) {
                Property property = getPropertiesIterator().nextProperty();
                ExtendedPropertyDefinition epd = node.getApplicablePropertyDefinition(property.getName());
                tempNext = new JCRPropertyWrapperImpl(node, property, node.getSession(), node.getProvider(), epd);
                return true;
            } else {
                do {
                    Property property = getI18NPropertyIterator().nextProperty();
                    final String name = property.getName();
                    try {
                        final ExtendedPropertyDefinition def = node.getApplicablePropertyDefinition(name);
                        if (def.isInternationalized()) {
                            tempNext = new JCRPropertyWrapperImpl(node, property, node.getSession(), node.getProvider(), def, name);
                            return true;
                        }
                    } catch (ConstraintViolationException e) {
                    }
                } while (true);
            }
        } catch (NoSuchElementException e) {
            return false;
        } catch (RepositoryException e) {
            throw new RuntimeException("nextProperty",e);
        }
    }

    public Object next() {
        return nextProperty();
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    public boolean containsKey(Object o) {
        try {
            return node.hasProperty( (String) o );       
        } catch (ConstraintViolationException e) {
            return false;
        } catch (RepositoryException e) {
            throw new RuntimeException("containsKey",e);
        }
    }

    public boolean containsValue(Object o) {
        throw new UnsupportedOperationException("containsValue");
    }

    public Object get(Object o) {
        try {
            Property p = node.getProperty( (String) o);

            if (p.isMultiple()) {
                return p.getValues();
            } else {
                return p.getValue();
            }
        } catch (PathNotFoundException e) {
            return null;
        } catch (ConstraintViolationException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException("get",e);
        }
    }

    public Object put(Object o, Object o1) {
        throw new UnsupportedOperationException("put");
    }

    public Object remove(Object o) {
        throw new UnsupportedOperationException("remove");
    }

    public void putAll(Map map) {
        throw new UnsupportedOperationException("putAll");
    }

    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    public Set keySet() {
        throw new UnsupportedOperationException("keySet");
    }

    public Collection values() {
        throw new UnsupportedOperationException("values");
    }

    public Set entrySet() {
        throw new UnsupportedOperationException("entrySet");
    }
}
