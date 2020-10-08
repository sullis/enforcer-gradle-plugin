/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 The author and/or original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package enforcer.rules

import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.kordamp.gradle.plugin.enforcer.api.AbstractEnforcerRule
import org.kordamp.gradle.plugin.enforcer.api.EnforcerContext
import org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase
import org.kordamp.gradle.plugin.enforcer.api.EnforcerRuleException

import static org.apache.commons.lang3.StringUtils.isNotBlank
import static org.kordamp.gradle.plugin.enforcer.api.EnforcerPhase.BEFORE_BUILD

/**
 * Contains the common code to compare an array of files against a requirement.
 *
 * Adapted from {@code org.apache.maven.plugins.enforcer.AbstractRequireFiles}
 * Original author: <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
abstract class AbstractRequireFiles extends AbstractEnforcerRule {
    final Property<String> message
    final Property<Boolean> allowNulls
    final ListProperty<File> files
    final ListProperty<EnforcerPhase> phases

    AbstractRequireFiles(ObjectFactory objects) {
        super(objects)
        message = objects.property(String)
        allowNulls = objects.property(Boolean).convention(false)
        files = objects.listProperty(File).convention([])
        phases = objects.listProperty(EnforcerPhase).convention([BEFORE_BUILD])
    }

    void file(String str) {
        if (isNotBlank(str)) {
            files.add(new File(str))
        }
    }

    void file(File file) {
        if (file) {
            files.add(file)
        }
    }

    @Override
    void execute(EnforcerContext context) throws EnforcerRuleException {
        if (context.enforcerPhase in phases.get()) {
            context.logger.debug("Enforcing rule ${resolveClassName()} on ${context}")
            doExecute(context)
        }
    }

    protected void doExecute(EnforcerContext context) throws EnforcerRuleException {
        if (!allowNulls.get() && files.get().size() == 0) {
            throw fail('The file list is empty and Null files are disabled.')
        }

        List<File> failures = []
        for (File file : files.get()) {
            if (!allowNulls && !file) {
                failures.add(file)
            } else if (!checkFile(context, file)) {
                failures.add(file)
            }
        }

        // if anything was found, log it with the optional message.
        if (failures) {
            String msg = message.orNull

            StringBuilder buf = new StringBuilder()
            if (isNotBlank(msg)) {
                buf.append(msg).append(System.lineSeparator())
            }
            buf.append(getErrorMsg()).append(System.lineSeparator())

            for (File file : failures) {
                if (file) {
                    buf.append(file.getAbsolutePath()).append(System.lineSeparator())
                } else {
                    buf.append('(an empty filename was given and allowNulls is false)').append(System.lineSeparator())
                }
            }

            throw fail(buf.toString())
        }
    }

    protected abstract boolean checkFile(EnforcerContext context, File file)

    protected abstract String getErrorMsg()
}