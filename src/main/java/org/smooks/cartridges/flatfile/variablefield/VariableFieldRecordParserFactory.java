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
package org.smooks.cartridges.flatfile.variablefield;

import org.smooks.api.ExecutionContext;
import org.smooks.api.Registry;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.bean.context.BeanContext;
import org.smooks.api.delivery.ContentHandlerBinding;
import org.smooks.api.delivery.VisitorAppender;
import org.smooks.api.delivery.ordering.Consumer;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.visitor.Visitor;
import org.smooks.api.resource.visitor.sax.ng.AfterVisitor;
import org.smooks.cartridges.flatfile.BindingType;
import org.smooks.cartridges.flatfile.FieldMetaData;
import org.smooks.cartridges.flatfile.RecordMetaData;
import org.smooks.cartridges.flatfile.RecordParserFactory;
import org.smooks.cartridges.javabean.Bean;
import org.smooks.engine.delivery.DefaultContentHandlerBinding;
import org.smooks.engine.expression.MVELExpressionEvaluator;
import org.smooks.support.XmlUtil;
import org.w3c.dom.Element;

import jakarta.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract VariableFieldRecordParserFactory.
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class VariableFieldRecordParserFactory implements RecordParserFactory, VisitorAppender {

    private static final String RECORD_BEAN = "recordBean";

    @Inject
    private Optional<String> fields;

    private VariableFieldRecordMetaData vfRecordMetaData;

    @Inject
    private Optional<String> recordDelimiter;
    private Pattern recordDelimiterPattern;

    @Inject
    private Boolean keepDelimiter = false;

    @Inject
    private String recordElementName = "record";

    @Inject
    private Optional<String> bindBeanId;

    @Inject
    private Optional<Class<?>> bindBeanClass;

    @Inject
    private Optional<BindingType> bindingType;
    
    @Inject
    private Optional<String> bindMapKeyField;

    @Inject
    @Named("skip-line-count")
    private Integer skipLines = 0;

    @Inject
    @Named("fields-in-message")
    private Boolean fieldsInMessage = false;

    @Inject
    private Boolean validateHeader = false;

    @Inject
    private Boolean strict = false;

    @Inject
    private Registry registry;
    
    private String overFlowFromLastRecord = "";

    public int getSkipLines() {
        if (skipLines < 0) {
            return 0;
        } else {
            return skipLines;
        }
    }

    public boolean fieldsInMessage() {
        return fieldsInMessage;
    }

    public boolean validateHeader() {
        return validateHeader;
    }

    /**
     * Get the default record element name.
     * 
     * @return The default record element name.
     */
    public String getRecordElementName() {
        return recordElementName;
    }

    public RecordMetaData getRecordMetaData() {
        return vfRecordMetaData.getRecordMetaData();
    }

    /**
     * Get the {@link RecordMetaData} instance for the specified fields.
     * @param fieldValues The fields.
     * @return The RecordMetaData instance.
     */
    public RecordMetaData getRecordMetaData(List<String> fieldValues) {
        return vfRecordMetaData.getRecordMetaData(fieldValues);
    }

    /**
     * Is the parser configured to parse multiple record types.
     * @return True if the parser configured to parse multiple record types, otherwise false.
     */
    public boolean isMultiTypeRecordSet() {
        return vfRecordMetaData.isMultiTypeRecordSet();
    }

    /**
     * Is this parser instance strict.
     * 
     * @return True if the parser is strict, otherwise false.
     */
    public boolean strict() {
        return strict;
    }

    @Override
    public List<ContentHandlerBinding<Visitor>> addVisitors() {
        List<ContentHandlerBinding<Visitor>> visitorBindings = new ArrayList<>(); 
        if (bindBeanId.isPresent() && bindBeanClass.isPresent()) {
            final Bean bean;

            if (fieldsInMessage) {
                throw new SmooksConfigException("Unsupported reader based bean binding config.  Not supported when fields are defined in message.  See 'fieldsInMessage' attribute.");
            }

            if (vfRecordMetaData.isMultiTypeRecordSet()) {
                throw new SmooksConfigException(
                        "Unsupported reader based bean binding config for a multi record type record set.  "
                                + "Only supported for single record type record sets.  Use <jb:bean> configs for multi binding record type record sets.");
            }

            if (BindingType.LIST.equals(bindingType.orElse(null))) {
                Bean listBean = new Bean(ArrayList.class, bindBeanId.get(), ResourceConfig.DOCUMENT_FRAGMENT_SELECTOR, registry);
                bean = listBean.newBean(bindBeanClass.get(), recordElementName);

                listBean.bindTo(bean);
                addFieldBindings(bean);

                visitorBindings.addAll(listBean.addVisitors());
            } else if (BindingType.MAP.equals(bindingType.orElse(null))) {
                if (!bindMapKeyField.isPresent()) {
                    throw new SmooksConfigException(
                            "'MAP' Binding must specify a 'keyField' property on the binding configuration.");
                }

                vfRecordMetaData.getRecordMetaData().assertValidFieldName(bindMapKeyField.get());

                Bean mapBean = new Bean(LinkedHashMap.class, bindBeanId.get(), ResourceConfig.DOCUMENT_FRAGMENT_SELECTOR, registry);
                Bean recordBean = new Bean(bindBeanClass.get(), RECORD_BEAN, recordElementName, registry);

                addFieldBindings(recordBean);
                visitorBindings.addAll(mapBean.addVisitors());
                visitorBindings.addAll(recordBean.addVisitors());
                
                MapBindingWiringVisitor wiringVisitor = new MapBindingWiringVisitor(bindMapKeyField.get(), bindBeanId.get());
                visitorBindings.add(new DefaultContentHandlerBinding<>(wiringVisitor, recordElementName, null, registry));
            } else {
                bean = new Bean(bindBeanClass.get(), bindBeanId.get(), recordElementName, registry);
                addFieldBindings(bean);

                visitorBindings.addAll(bean.addVisitors());
            }
        }
        
        return visitorBindings;
    }

    @PostConstruct
    public final void fixupRecordDelimiter() {
        if (!recordDelimiter.isPresent()) {
            return;
        }

        // Fixup the record delimiter...
        if (recordDelimiter.get().startsWith("regex:")) {
            recordDelimiterPattern = Pattern.compile(recordDelimiter.get().substring("regex:".length()),
                    (Pattern.MULTILINE | Pattern.DOTALL));
        } else {
            recordDelimiter = Optional.of(removeSpecialCharEncodeString(recordDelimiter.get(), "\\n", '\n'));
            recordDelimiter = Optional.of(removeSpecialCharEncodeString(recordDelimiter.get(), "\\r", '\r'));
            recordDelimiter = Optional.of(removeSpecialCharEncodeString(recordDelimiter.get(), "\\t", '\t'));
            recordDelimiter = Optional.ofNullable(XmlUtil.removeEntities(recordDelimiter.get()));
        }
    }

    @PostConstruct
    public final void buildRecordMetaData() {
        vfRecordMetaData = new VariableFieldRecordMetaData(recordElementName, fields.orElse(null));
    }

    /**
     * Read a record from the specified reader (up to the next recordDelimiter).
     * 
     * @param recordReader The record {@link Reader}.
     * @param recordBuffer The record buffer into which the record is read.
     * @throws IOException Error reading record.
     */
    public void readRecord(Reader recordReader, StringBuilder recordBuffer, int recordNumber) throws IOException {
        recordBuffer.setLength(0);
        recordBuffer.append(overFlowFromLastRecord);

        RecordBoundaryLocator boundaryLocator;
        if (recordDelimiterPattern != null) {
            boundaryLocator = new RegexRecordBoundaryLocator(recordBuffer, recordNumber);
        } else {
            boundaryLocator = new SimpleRecordBoundaryLocator(recordBuffer, recordNumber);
        }

        int c;
        while ((c = recordReader.read()) != -1) {
            if (recordBuffer.length() == 0) {
                if (c == '\n' || c == '\r') {
                    // A leading CR or LF... ignore...
                    continue;
                }
            }

            recordBuffer.append((char) c);
            if (boundaryLocator.atEndOfRecord()) {
                break;
            }
        }

        overFlowFromLastRecord = boundaryLocator.getOverflowCharacters();
    }

    private void addFieldBindings(Bean bean) {
        for (FieldMetaData fieldMetaData : vfRecordMetaData.getRecordMetaData().getFields()) {
            if (!fieldMetaData.ignore()) {
                bean.bindTo(fieldMetaData.getName(), recordElementName + "/" + fieldMetaData.getName());
            }
        }
    }

    private static String removeSpecialCharEncodeString(String string, String encodedString, char replaceChar) {
        return string.replace(encodedString, new String(new char[] { replaceChar }));
    }

    private class MapBindingWiringVisitor implements AfterVisitor, Consumer {

        private MVELExpressionEvaluator keyExtractor = new MVELExpressionEvaluator();
        private String mapBindingKey;

        private MapBindingWiringVisitor(String bindKeyField, String mapBindingKey) {
            keyExtractor.setExpression(RECORD_BEAN + "." + bindKeyField);
            this.mapBindingKey = mapBindingKey;
        }
        
        @Override
        public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
            wireObject(executionContext);
        }

        private void wireObject(ExecutionContext executionContext) {
            BeanContext beanContext = executionContext.getBeanContext();
            Map<String, Object> beanMap = beanContext.getBeanMap();
            Object key = keyExtractor.getValue(beanMap);

            @SuppressWarnings("unchecked")
            // TODO: Optimize to use the BeanId object
            Map<Object, Object> map = (Map<Object, Object>) beanContext.getBean(mapBindingKey);
            Object record = beanContext.getBean(RECORD_BEAN);

            map.put(key, record);
        }

        public boolean consumes(Object object) {
            if (keyExtractor.getExpression().contains(object.toString())) {
                return true;
            }

            return false;
        }
    }

    private class SimpleRecordBoundaryLocator extends RecordBoundaryLocator {

        private SimpleRecordBoundaryLocator(StringBuilder recordBuffer, int recordNumber) {
            super(recordBuffer, recordNumber);
        }

        @Override
        boolean atEndOfRecord() {
            int builderLen = recordBuffer.length();
            char lastChar = recordBuffer.charAt(builderLen - 1);

            if (recordDelimiter.isPresent()) {
                int stringLen = recordDelimiter.get().length();

                if (builderLen < stringLen) {
                    return false;
                }

                int stringIndx = 0;
                for (int i = (builderLen - stringLen); i < builderLen; i++) {
                    if (recordBuffer.charAt(i) != recordDelimiter.get().charAt(stringIndx)) {
                        return false;
                    }
                    stringIndx++;
                }

                if (!keepDelimiter) {
                    // Strip off the delimiter from the end before returning...
                    recordBuffer.setLength(builderLen - stringLen);
                }

                return true;
            } else if (lastChar == '\r' || lastChar == '\n') {
                if (!keepDelimiter) {
                    // Strip off the delimiter from the end before returning...
                    recordBuffer.setLength(builderLen - 1);
                }
                return true;
            }

            return false;
        }

        @Override
        String getOverflowCharacters() {
            return "";
        }
    }

    private class RegexRecordBoundaryLocator extends RecordBoundaryLocator {

        private int startFindIndex;
        private int endRecordIndex;
        private String overFlow = "";

        protected RegexRecordBoundaryLocator(StringBuilder recordBuffer, int recordNumber) {
            super(recordBuffer, recordNumber);
            startFindIndex = recordBuffer.length();
        }

        @Override
        boolean atEndOfRecord() {
            Matcher matcher = recordDelimiterPattern.matcher(recordBuffer);

            if (matcher.find(startFindIndex)) {
                if (recordNumber == 1 && startFindIndex == 0) {
                    // Need to find the second instance of the pattern in the
                    // first record buffer
                    // The second instance marks the start of the second record,
                    // which will be captured
                    // as overflow from this record read and will be auto added
                    // to the read of the next record.
                    startFindIndex = matcher.end();
                    return false;
                } else {
                    // For records following the first record, we already have
                    // the start so we just need to find
                    // the first instance of the pattern, which marks the start
                    // of the next record, which again
                    // will be captured as overflow from this record read and
                    // will be auto added to the read of
                    // the next record.
                    endRecordIndex = matcher.start();
                    overFlow = recordBuffer.substring(endRecordIndex);
                    recordBuffer.setLength(endRecordIndex);
                    return true;
                }
            }

            return false;
        }

        @Override
        String getOverflowCharacters() {
            return overFlow;
        }
    }

    private abstract class RecordBoundaryLocator {

        protected StringBuilder recordBuffer;
        protected int recordNumber;

        protected RecordBoundaryLocator(StringBuilder recordBuffer, int recordNumber) {
            this.recordBuffer = recordBuffer;
            this.recordNumber = recordNumber;
        }

        abstract boolean atEndOfRecord();

        abstract String getOverflowCharacters();
    }
}
