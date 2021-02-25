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

import org.smooks.api.SmooksConfigException;
import org.smooks.assertion.AssertArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * Record metadata.
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class RecordMetaData {

    private final String name;
    private final List<FieldMetaData> fields;
    private final boolean wildCardRecord;
    private int ignoredFieldCount;
    private int unignoredFieldCount;
    private List<String> fieldNames;

    /**
     * public constructor.
     * 
     * @param name Record name.
     * @param fields Record fields metadata.
     */
    public RecordMetaData(String name, List<FieldMetaData> fields) {
        this(name, fields, false);
    }

    /**
     * public constructor.
     * 
     * @param name Record name.
     * @param fields Record fields metadata.
     * @param wildCardRecord Wildcard record. Accept any fields and generate the
     *        field names based on index.
     */
    public RecordMetaData(String name, List<FieldMetaData> fields, boolean wildCardRecord) {
        AssertArgument.isNotNullAndNotEmpty(name, "name");
        AssertArgument.isNotNull(fields, "fields");
        this.name = name.trim();
        this.fields = fields;
        this.wildCardRecord = wildCardRecord;
        countIgnoredFields();
        gatherFieldNames();
    }

    /**
     * Get the record name.
     * 
     * @return The record name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the record fields metadata.
     * 
     * @return The record fields metadata.
     */
    public List<FieldMetaData> getFields() {
        return fields;
    }

    /**
     * Is this a wildcard record.
     * <p/>
     * If it is, accept all fields and use the field index to generate the field
     * name.
     * 
     * @return True of this is a wildcard record, otherwise false.
     */
    public boolean isWildCardRecord() {
        return wildCardRecord;
    }

    /**
     * Get the number of fields in this record that are ignored.
     * 
     * @return The number of fields in this record that are ignored.
     */
    public int getIgnoredFieldCount() {
        return ignoredFieldCount;
    }

    /**
     * Get the number of fields in this record that are not ignored.
     * 
     * @return The number of fields in this record that are not ignored.
     */
    public int getUnignoredFieldCount() {
        return unignoredFieldCount;
    }

    /**
     * Get a collection of all the field names (excluding ignored fields) in
     * this record.
     * 
     * @return Acollection of all the field names in this record.
     */
    public List<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * Assert that the supplied field name is one of the field names associated
     * with this record.
     * 
     * @param fieldName The field name to test.
     */
    public void assertValidFieldName(String fieldName) {
        if (!fieldNames.contains(fieldName)) {
            throw new SmooksConfigException("Invalid field name '" + fieldName + "'.  Valid names: "
                    + fieldNames + ".");
        }
    }

    private void countIgnoredFields() {
        for (FieldMetaData field : fields) {
            if (field.ignore()) {
                ignoredFieldCount++;
            } else {
                unignoredFieldCount++;
            }
        }
    }

    private void gatherFieldNames() {
        if (fields == null) {
            fieldNames = new ArrayList<String>();
        }

        fieldNames = new ArrayList<String>();

        for (FieldMetaData field : fields) {
            if (!field.ignore()) {
                fieldNames.add(field.getName());
            }
        }
    }
}
