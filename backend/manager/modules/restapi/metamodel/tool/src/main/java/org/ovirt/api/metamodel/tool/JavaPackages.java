/*
Copyright (c) 2015 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.ovirt.api.metamodel.tool;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;

import org.ovirt.api.metamodel.concepts.Module;
import org.ovirt.api.metamodel.concepts.Name;

/**
 * This class contains the rules used to calculate the names of generated Java packages.
 */
@ApplicationScoped
public class JavaPackages {
    /**
     * The list of rules used to transform JAX-RS package names.
     */
    private List<ReplacementRule> jaxrsRules = new ArrayList<>();

    public void addJaxrsRule(String rule) {
        addRule(jaxrsRules, rule);
    }

    private void addRule(List<ReplacementRule> rules, String text) {
        int index = text.indexOf('=');
        if (index >= 0) {
            String expression = text.substring(0, index);
            String replacement = text.substring(index + 1);
            ReplacementRule rule = new ReplacementRule(expression, replacement);
            rules.add(rule);
        }
        else {
            ReplacementRule rule = new ReplacementRule("^.*$", text);
            rules.add(rule);
        }
    }

    /**
     * The name of the package that contains the classes generated by the XJC compiler.
     */
    private String xjcPackageName = "org.ovirt.engine.api.model";

    public String getXjcPackageName() {
        return xjcPackageName;
    }

    public void setXjcPackageName(String newXjcPackageName) {
        xjcPackageName = newXjcPackageName;
    }

    public String getJaxrsPackageName() {
        return getPackageName(jaxrsRules, "");
    }

    public String getJaxrsPackageName(Module module) {
        return getPackageName(jaxrsRules, module);
    }

    private String getPackageName(List<ReplacementRule> rules, Module module) {
        return getPackageName(rules, module.getName());
    }

    private String getPackageName(List<ReplacementRule> rules, Name name) {
        String text = name.words().collect(joining("."));
        return getPackageName(rules, text);
    }

    private String getPackageName(List<ReplacementRule> rules, String text) {
        // Process the rules, the first that matches will be applied and the result returned:
        for (ReplacementRule rule : rules) {
            String result = rule.process(text);
            if (result != null) {
                return result;
            }
        }

        // If no rule matches then return the text as is:
        return text;
    }
}

