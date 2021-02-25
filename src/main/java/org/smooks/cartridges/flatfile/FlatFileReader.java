/*-
 * ========================LICENSE_START=================================
 * smooks-flatfile-cartridge
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 * 
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 * 
 * ======================================================================
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * ======================================================================
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.cartridges.flatfile;

import org.apache.commons.lang.StringUtils;
import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.delivery.ContentHandlerBinding;
import org.smooks.api.delivery.VisitorAppender;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.reader.SmooksXMLReader;
import org.smooks.api.resource.visitor.Visitor;
import org.smooks.engine.injector.Scope;
import org.smooks.engine.lifecycle.PostConstructLifecyclePhase;
import org.smooks.engine.lookup.LifecycleManagerLookup;
import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.XMLConstants;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Flat file reader.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@SuppressWarnings("unchecked")
public class FlatFileReader implements SmooksXMLReader, VisitorAppender {
    private static Attributes EMPTY_ATTRIBS = new AttributesImpl();

    private static final char[] INDENT_LF = new char[] {'\n'};
    private static final char[] INDENTCHARS = new char[] {'\t', '\t'};
    private static final String RECORD_NUMBER_ATTR = "number";
    private static final String RECORD_TRUNCATED_ATTR = "truncated";

    private ContentHandler contentHandler;
	private ExecutionContext execContext;

    @Inject
    private ResourceConfig resourceConfig;

    @Inject
    private ApplicationContext appContext;

    @Inject
    @Named("parserFactory")
    private Class<? extends RecordParserFactory> parserFactoryClass;
    private RecordParserFactory parserFactory;

    @Inject
    private String rootElementName = "records";

    @Inject
    private Boolean indent = false;

	@PostConstruct
	public void initialize() throws IllegalAccessException, InstantiationException {
        parserFactory = parserFactoryClass.newInstance();
        appContext.getRegistry().lookup(new LifecycleManagerLookup()).applyPhase(parserFactory, new PostConstructLifecyclePhase(new Scope(appContext.getRegistry(), resourceConfig, parserFactory)));
	}

	@Override
    public List<ContentHandlerBinding<Visitor>> addVisitors() {
        if(parserFactory instanceof VisitorAppender) {
            return ((VisitorAppender) parserFactory).addVisitors();
        } else {
            return Collections.emptyList();
        }
    }

    /* (non-Javadoc)
	 * @see org.smooks.xml.SmooksXMLReader#setExecutionContext(org.smooks.container.ExecutionContext)
	 */
	public void setExecutionContext(ExecutionContext request) {
		this.execContext = request;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#parse(org.xml.sax.InputSource)
	 */
	public void parse(InputSource inputSource) throws IOException, SAXException {
        if(contentHandler == null) {
            throw new IllegalStateException("'contentHandler' not set.  Cannot parse Record stream.");
        }
        if(execContext == null) {
            throw new IllegalStateException("'execContext' not set.  Cannot parse Record stream.");
        }

        try {
            // Create the record parser....
            RecordParser recordParser = parserFactory.newRecordParser();
            recordParser.setRecordParserFactory(parserFactory);
            recordParser.setDataSource(inputSource);

            try {
                recordParser.initialize();

                // Start the document and add the root "record-set" element...
                contentHandler.startDocument();
                contentHandler.startElement(XMLConstants.NULL_NS_URI, rootElementName, StringUtils.EMPTY, EMPTY_ATTRIBS);

                // Output each of the CVS line entries...
                int lineNumber = 0;

                Record record = recordParser.nextRecord();
                while (record != null) {
                    lineNumber++; // First line is line "1"

                    List<Field> recordFields = record.getFields();

                    if(indent) {
                        contentHandler.characters(INDENT_LF, 0, 1);
                        contentHandler.characters(INDENTCHARS, 0, 1);
                    }

                    AttributesImpl attrs = new AttributesImpl();
                    attrs.addAttribute(XMLConstants.NULL_NS_URI, RECORD_NUMBER_ATTR, RECORD_NUMBER_ATTR, "xs:int", Integer.toString(lineNumber));

                    RecordMetaData recordMetaData = record.getRecordMetaData();
                    if(recordFields.size() < recordMetaData.getUnignoredFieldCount()) {
                        attrs.addAttribute(XMLConstants.NULL_NS_URI, RECORD_TRUNCATED_ATTR, RECORD_TRUNCATED_ATTR, "xs:boolean", Boolean.TRUE.toString());
                    }

                    contentHandler.startElement(XMLConstants.NULL_NS_URI, record.getName(), StringUtils.EMPTY, attrs);
                    for(Field recordField : recordFields) {
                        String fieldName = recordField.getName();

                        if(indent) {
                            contentHandler.characters(INDENT_LF, 0, 1);
                            contentHandler.characters(INDENTCHARS, 0, 2);
                        }

                        contentHandler.startElement(XMLConstants.NULL_NS_URI, fieldName, StringUtils.EMPTY, EMPTY_ATTRIBS);

                        String value = recordField.getValue();
                        contentHandler.characters(value.toCharArray(), 0, value.length());
                        contentHandler.endElement(XMLConstants.NULL_NS_URI, fieldName, StringUtils.EMPTY);
                    }

                    if(indent) {
                        contentHandler.characters(INDENT_LF, 0, 1);
                        contentHandler.characters(INDENTCHARS, 0, 1);
                    }

                    contentHandler.endElement(XMLConstants.NULL_NS_URI, record.getName(), StringUtils.EMPTY);

                    record = recordParser.nextRecord();
                }

                if(indent) {
                    contentHandler.characters(INDENT_LF, 0, 1);
                }

                // Close out the "csv-set" root element and end the document..
                contentHandler.endElement(XMLConstants.NULL_NS_URI, rootElementName, StringUtils.EMPTY);
                contentHandler.endDocument();
            } finally {
                recordParser.uninitialize();
            }
        } finally {
        	// These properties need to be reset for every execution (e.g. when reader is pooled).
        	contentHandler = null;
        	execContext = null;
        }
	}

    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    /****************************************************************************
     *
     * The following methods are currently unimplemnted...
     *
     ****************************************************************************/

    public void parse(String systemId)
    {
        throw new UnsupportedOperationException("Operation not supports by this reader.");
    }

    public boolean getFeature(String name)
    {
        return false;
    }

    public void setFeature(String name, boolean value)
    {
    }

    public DTDHandler getDTDHandler() {
        return null;
    }

    public void setDTDHandler(DTDHandler arg0) {
    }

    public EntityResolver getEntityResolver() {
        return null;
    }

    public void setEntityResolver(EntityResolver arg0) {
    }

    public ErrorHandler getErrorHandler() {
        return null;
    }

    public void setErrorHandler(ErrorHandler arg0) {
    }

    public Object getProperty(String name)
    {
        return null;
    }

    public void setProperty(String name, Object value)
    {
    }
}
