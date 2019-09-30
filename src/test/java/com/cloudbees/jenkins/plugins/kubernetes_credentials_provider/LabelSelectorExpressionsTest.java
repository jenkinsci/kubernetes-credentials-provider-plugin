package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import static org.junit.Assert.*;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LabelSelectorExpressionsTest {

    public @Rule ExpectedException thrown = ExpectedException.none();

    @Test
    public void parse() throws LabelSelectorParseException {
        LabelSelector selector = LabelSelectorExpressions.parse("mycompany.com/partition  in  (customerA, customerB),environment!=qa,!foo,bar,color=blue,bingo notin (barn),owner in ( john , mary )");

        LabelSelector expected = new LabelSelectorBuilder()
                .addToMatchLabels("color", "blue")
                .addNewMatchExpression()
                    .withKey("mycompany.com/partition")
                    .withOperator("In")
                    .withValues("customerA", "customerB")
                    .endMatchExpression()
                .addNewMatchExpression()
                    .withKey("environment")
                    .withOperator("NotIn")
                    .withValues("qa")
                    .endMatchExpression()
                .addNewMatchExpression()
                    .withKey("foo")
                    .withOperator("DoesNotExist")
                    .endMatchExpression()
                .addNewMatchExpression()
                    .withKey("bar")
                    .withOperator("Exists")
                    .endMatchExpression()
                .addNewMatchExpression()
                    .withKey("bingo")
                    .withOperator("NotIn")
                    .withValues("barn")
                    .endMatchExpression()
                .addNewMatchExpression()
                    .withKey("owner")
                    .withOperator("In")
                    .withValues("john", "mary")
                    .endMatchExpression()
                .build();

        assertEquals(expected, selector);
    }

    @Test
    public void parseInvalidOperator() throws LabelSelectorParseException {
        thrown.expect(LabelSelectorParseException.class);
        thrown.expectMessage("Unrecognized selector operator 'of' in expression");
        LabelSelectorExpressions.parse("partition  of  (customerA, customerB)");
    }

    @Test
    public void parseInvalidExpressionTokenCountTwo() throws LabelSelectorParseException {
        thrown.expect(LabelSelectorParseException.class);
        thrown.expectMessage("Invalid selector expression 'partition  in'. Expected 1 or 3 tokens, got 2: [partition, in]");
        LabelSelectorExpressions.parse("partition  in");
    }
}