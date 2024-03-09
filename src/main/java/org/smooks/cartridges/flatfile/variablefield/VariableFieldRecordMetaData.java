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

import org.smooks.api.SmooksConfigException;
import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.flatfile.FieldMetaData;
import org.smooks.cartridges.flatfile.RecordMetaData;
import org.smooks.cartridges.flatfile.function.StringFunctionExecutor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variable fields record metadata.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class VariableFieldRecordMetaData {

    public static final Pattern SINGLE_RECORD_PATTERN = Pattern.compile("^[\\w|[?$-_, ]]+$");
    public static final Pattern MULTI_RECORD_PATTERN = Pattern.compile("^([\\w|[?$-_*]]+)\\[([\\w|[?$-_, *]]+)\\]$");

    private RecordMetaData recordMetaData; // Initialized if there's only one
    // record type defined
    private Map<String, RecordMetaData> recordMetaDataMap; // Initialized if
    // there's multiple
    // record types
    // defined

    public static RecordMetaData UNKNOWN_RECORD_TYPE;

    static {
        UNKNOWN_RECORD_TYPE = new RecordMetaData("UNMATCHED", new ArrayList<FieldMetaData>());
        UNKNOWN_RECORD_TYPE.getFields().add(new FieldMetaData("value"));
    }

    /**
     * Construct a Variable Field record metadata set.
     *
     * @param recordElementName The record name.  Can be <code>null</code>.
     * @param fields            The fields definition.
     */
    public VariableFieldRecordMetaData(String recordElementName, String fields) {
        if (fields == null) {
            recordMetaData = new RecordMetaData(recordElementName, new ArrayList<FieldMetaData>(), true);
        } else {
            List<String> recordDefs = Arrays.asList(fields.split("\\|"));

            for (int i = 0; i < recordDefs.size(); i++) {
                recordDefs.set(i, recordDefs.get(i).trim());
            }

            if (recordDefs.size() == 1) {
                if (SINGLE_RECORD_PATTERN.matcher(recordDefs.get(0)).matches()) {
                    recordMetaData = buildRecordMetaData(recordElementName, recordDefs.get(0).split(","));
                    return;
                }

                recordMetaData = buildMultiRecordMetaData(recordDefs.get(0));
                if (recordMetaData == null) {
                    throw new SmooksConfigException("Unsupported fields definition '" + fields
                            + "'.  Must match either the single ('" + SINGLE_RECORD_PATTERN.pattern()
                            + "') or multi ('" + MULTI_RECORD_PATTERN.pattern() + "') record pattern.");
                }
            } else {
                for (String recordDef : recordDefs) {
                    recordDef = recordDef.trim();
                    RecordMetaData multiRecordMetaData = buildMultiRecordMetaData(recordDef);
                    if (multiRecordMetaData == null) {
                        throw new SmooksConfigException("Unsupported fields definition '" + recordDef
                                + "'.  Must match the multi record pattern ('" + MULTI_RECORD_PATTERN.pattern()
                                + "') .");
                    }
                    if (recordMetaDataMap == null) {
                        recordMetaDataMap = new HashMap<String, RecordMetaData>();
                    }
                    recordMetaDataMap.put(multiRecordMetaData.getName(), multiRecordMetaData);
                }
            }
        }
    }

    /**
     * Is this a parser factory for a multi-record type data stream.
     *
     * @return True if this is a parser factory for a multi-record type data
     * stream, otherwise false.
     */
    public boolean isMultiTypeRecordSet() {
        return (recordMetaData == null && recordMetaDataMap != null);
    }

    /**
     * Get the record metadata for the variable field record parser.
     *
     * @return The record metadata.
     * @see #isMultiTypeRecordSet()
     */
    public RecordMetaData getRecordMetaData() {
        if (isMultiTypeRecordSet()) {
            throw new IllegalStateException(
                    "Invalid call to getRecordMetaData().  This is a multi-type record set.  Must call getRecordMetaData(String recordTypeName).");
        }
        return recordMetaData;
    }

    /**
     * Get the record metadata for the variable field record parser.
     *
     * @param recordTypeName The name of the record type.
     * @return The record metadata.
     * @see #isMultiTypeRecordSet()
     */
    public RecordMetaData getRecordMetaData(String recordTypeName) {
        AssertArgument.isNotNullAndNotEmpty(recordTypeName, "recordTypeName");
        if (!isMultiTypeRecordSet()) {
            throw new IllegalStateException(
                    "Invalid call to getRecordMetaData(String recordTypeName).  This is not a multi-type record set.  Must call getRecordMetaData().");
        }
        return recordMetaDataMap.get(recordTypeName);
    }

    /**
     * Get the record metadata for the record.
     *
     * @param record The record.
     * @return The record metadata.
     * @see #isMultiTypeRecordSet()
     */
    public RecordMetaData getRecordMetaData(String[] record) {
        AssertArgument.isNotNullAndNotEmpty(record, "record");
        if (!isMultiTypeRecordSet()) {
            return recordMetaData;
        } else {
            return recordMetaDataMap.get(record[0].trim());
        }
    }

    /**
     * Get the record metadata for the record.
     *
     * @param record The record.
     * @return The record metadata.
     * @see #isMultiTypeRecordSet()
     */
    public RecordMetaData getRecordMetaData(Collection<String> record) {
        AssertArgument.isNotNullAndNotEmpty(record, "record");
        if (!isMultiTypeRecordSet()) {
            return recordMetaData;
        } else {
            RecordMetaData vrecordMetaData = recordMetaDataMap.get(record.iterator().next().trim());
            if (vrecordMetaData == null) {
                vrecordMetaData = UNKNOWN_RECORD_TYPE;
            }
            return vrecordMetaData;
        }
    }

    public RecordMetaData buildMultiRecordMetaData(String recordDef) {
        Matcher matcher = MULTI_RECORD_PATTERN.matcher(recordDef);

        if (matcher.matches()) {
            return buildRecordMetaData(matcher.group(1), matcher.group(2).split(","));
        }

        return null;
    }

    private RecordMetaData buildRecordMetaData(String recordName, String[] fieldNames) {
        return buildRecordMetaData(recordName, Arrays.asList(fieldNames));
    }

    public static RecordMetaData buildRecordMetaData(String recordName, List<String> fieldNames) {
        // Parse input fields to extract names and lengths
        List<FieldMetaData> fieldsMetaData = new ArrayList<FieldMetaData>();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldSpec = fieldNames.get(i).trim();

            if (fieldSpec.equals("*")) {
                return new RecordMetaData(recordName, fieldsMetaData, true);
            } else {
                FieldMetaData fieldMetaData;
                if (fieldSpec.indexOf('?') >= 0) {
                    String fieldName = fieldSpec.substring(0, fieldSpec.indexOf('?'));
                    String functionDefinition = fieldSpec.substring(fieldSpec.indexOf('?') + 1);

                    fieldMetaData = new FieldMetaData(fieldName);
                    if (functionDefinition.length() != 0) {
                        fieldMetaData.setStringFunctionExecutor(StringFunctionExecutor.getInstance(functionDefinition));
                    }
                } else {
                    fieldMetaData = new FieldMetaData(fieldSpec);
                }

                fieldsMetaData.add(fieldMetaData);
                if (fieldMetaData.ignore() && fieldMetaData.getIgnoreCount() > 1
                        && fieldMetaData.getIgnoreCount() < Integer.MAX_VALUE) {
                    // pad out with an FieldMetaData instance for each
                    // additionally ignored
                    // field in the record...
                    for (int ii = 0; ii < fieldMetaData.getIgnoreCount() - 1; ii++) {
                        fieldsMetaData.add(new FieldMetaData(FieldMetaData.IGNORE_FIELD));
                    }
                }
            }
        }

        return new RecordMetaData(recordName, fieldsMetaData);
    }
}
