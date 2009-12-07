package com.xmlcalabash.util;

import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.transform.URIResolver;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.Configuration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 29, 2008
 * Time: 4:04:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class XProcURIResolver implements URIResolver, EntityResolver {
    private URIResolver uriResolver = null;
    private EntityResolver entityResolver = null;
    private XProcRuntime runtime = null;
    private Hashtable<String,XdmNode> cache = new Hashtable<String,XdmNode> ();
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public XProcURIResolver(XProcRuntime runtime) {
        this.runtime = runtime;
    }

    public void setUnderlyingURIResolver(URIResolver resolver) {
        this.uriResolver = resolver;
    }

    public void setUnderlyingEntityResolver(EntityResolver resolver) {
        this.entityResolver = resolver;
    }

    public void cache(XdmNode doc, URI baseURI) {
        XdmNode root = S9apiUtils.getDocumentElement(doc);

        // We explicitly use the base URI of the root element so that if it has an xml:base
        // attribute, that becomes the base URI of the document.

        URI docURI = baseURI.resolve(root.getBaseURI());
        cache.put(docURI.toASCIIString(), doc);
    }

    public Source resolve(String href, String base) throws TransformerException {
        runtime.finest(logger,null,"URIResolver(" + href + "," + base + ")");

        try {
            URI baseURI = new URI(base);
            String uri = baseURI.resolve(href).toASCIIString();
            if (cache.containsKey(uri)) {
                runtime.finest(logger,null,"Returning cached document.");
                return cache.get(uri).asSource();
            }

            if (runtime.getSafeMode() && uri.startsWith("file:")) {
                throw XProcException.dynamicError(21);
            }
        } catch (URISyntaxException use) {
            runtime.finest(logger,null,"URISyntaxException resolving base and href?");
        }

        if (uriResolver != null) {
            Source resolved = uriResolver.resolve(href, base);
            // FIXME: This is a grotesque hack. This is wrong. Wrong. Wrong.
            // To support caching, XMLResolver (xmlresolver.org) returns a Source even when it hasn't
            // found the resource. Problem is, it doesn't setup the entity resolver correctly for that
            // resource. So we hack at it here...
            if (resolved != null && resolved instanceof SAXSource) {
                SAXSource ssource = (SAXSource) resolved;
                XMLReader reader = ssource.getXMLReader();
                if (reader == null) {
                    try {
                        reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                        reader.setEntityResolver(this);
                        ssource.setXMLReader(reader);
                    } catch (SAXException se) {
                        // nop?
                    } catch (ParserConfigurationException pce) {
                        // nop?
                    }
                }
            }
            return resolved;
        }
        
        return null;
    }

    public XdmNode parse(String href, String base) {
        return parse(href, base, false);
    }

    public XdmNode parse(String href, String base, boolean dtdValidate) {
        Source source = null;

        href = URIUtils.encode(href);

        runtime.finest(logger,null,"Attempting to parse: " + href + " (" + base + ")");

        try {
            source = resolve(href, base);
        } catch (TransformerException te) {
            throw new XProcException(XProcConstants.dynamicError(9), te);
        }

        if (source == null) {
            try {
                URI baseURI = new URI(base);
                source = new SAXSource(new InputSource(baseURI.resolve(href).toASCIIString()));
            } catch (URISyntaxException use) {
                throw new XProcException(use);
            }
        }

        DocumentBuilder builder = runtime.getProcessor().newDocumentBuilder();
        builder.setDTDValidation(dtdValidate);
        builder.setLineNumbering(true);
        //builder.setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy.NONE);

        Processor proc = runtime.getProcessor();
        Configuration config = proc.getUnderlyingConfiguration();

        try {
            return builder.build(source);
        } catch (SaxonApiException sae) {
            if (sae.getMessage().contains("validation")) {
                throw XProcException.stepError(27, sae);
            } else {
                throw XProcException.dynamicError(11, sae);
            }
        }
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        runtime.finest(logger,null,"ResolveEntity(" + publicId + "," + systemId + ")");

        try {
            URI baseURI = new URI(systemId);
            String uri = baseURI.toASCIIString();
            if (cache.containsKey(uri)) {
                runtime.finest(logger,null,"Returning cached document.");
                return S9apiUtils.xdmToInputSource(runtime, cache.get(uri));
            }
        } catch (URISyntaxException use) {
            runtime.finest(logger,null,"URISyntaxException resolving entityResolver systemId: " + systemId);
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        if (entityResolver != null) {
            InputSource r = entityResolver.resolveEntity(publicId, systemId);
            return r;
        } else {
            return null;
        }
    }
}
