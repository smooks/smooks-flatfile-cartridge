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
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.cartridges.flatfile.Binding;
import org.smooks.cartridges.flatfile.BindingType;
import org.smooks.cartridges.flatfile.FlatFileReader;
import org.smooks.engine.resource.config.GenericReaderConfigurator;

import java.util.List;

/**
 * Abstract Variable Field Record Parser configurator.
 *
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public abstract class VariableFieldRecordParserConfigurator extends GenericReaderConfigurator {

    private Class<? extends VariableFieldRecordParserFactory> factoryParserClass;
    private boolean indent = false;
    private boolean strict = true;
    private boolean fieldsInMessage = false;
    private Binding binding;

    public VariableFieldRecordParserConfigurator(Class<? extends VariableFieldRecordParserFactory> factoryParserClass) {
        super(FlatFileReader.class);
        this.factoryParserClass = factoryParserClass;
    }

    public VariableFieldRecordParserConfigurator setIndent(boolean indent) {
        this.indent = indent;
        return this;
    }

    public VariableFieldRecordParserConfigurator setStrict(boolean strict) {
        this.strict = strict;
        return this;
    }

    public VariableFieldRecordParserConfigurator setFieldsInMessage(boolean fieldsInMessage) {
        this.fieldsInMessage = fieldsInMessage;
        return this;
    }

    public VariableFieldRecordParserConfigurator setBinding(Binding binding) {
        this.binding = binding;
        return this;
    }

    public List<ResourceConfig> toConfig() {
        getParameters().setProperty("parserFactory", factoryParserClass.getName());
        getParameters().setProperty("indent", Boolean.toString(indent));
        getParameters().setProperty("strict", Boolean.toString(strict));
        getParameters().setProperty("fields-in-message", Boolean.toString(fieldsInMessage));

        if (binding != null) {
            getParameters().setProperty("bindBeanId", binding.getBeanId());
            getParameters().setProperty("bindBeanClass", binding.getBeanClass().getName());
            getParameters().setProperty("bindingType", binding.getBindingType().toString());
            if (binding.getBindingType() == BindingType.MAP) {
                if (binding.getKeyField() == null) {
                    throw new SmooksConfigException("CSV 'MAP' Binding must specify a 'keyField' property on the binding configuration.");
                }
                getParameters().setProperty("bindMapKeyField", binding.getKeyField());
            }
        }

        return super.toConfig();
    }
}
