package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelSelectorExpressionsTest {

	@Test
	void parse() throws LabelSelectorParseException {
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
	void parseInvalidOperator() {
		LabelSelectorParseException exception = assertThrows(LabelSelectorParseException.class, () ->
				LabelSelectorExpressions.parse("partition  of  (customerA, customerB)"));
		assertTrue(exception.getMessage().contains("Unrecognized selector operator 'of' in expression"));
	}

	@Test
	void parseInvalidExpressionTokenCountTwo() {
		LabelSelectorParseException exception = assertThrows(LabelSelectorParseException.class, () ->
				LabelSelectorExpressions.parse("partition  in"));
		assertTrue(exception.getMessage().contains("Invalid selector expression 'partition  in'. Expected 1 or 3 tokens, got 2: [partition, in]"));
	}
}