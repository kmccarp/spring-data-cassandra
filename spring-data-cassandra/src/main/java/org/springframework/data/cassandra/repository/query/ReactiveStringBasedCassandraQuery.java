/*
 * Copyright 2016-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.cassandra.repository.query;

import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

import com.datastax.oss.driver.api.core.cql.SimpleStatement;

/**
 * String-based {@link AbstractReactiveCassandraQuery} implementation.
 * <p>
 * A {@link ReactiveStringBasedCassandraQuery} expects a query method to be annotated with
 * {@link org.springframework.data.cassandra.repository.Query} with a CQL query. String-based queries support named,
 * index-based and expression parameters that are resolved during query execution.
 *
 * @author Mark Paluch
 * @see org.springframework.data.cassandra.repository.Query
 * @see org.springframework.data.cassandra.repository.query.AbstractReactiveCassandraQuery
 * @since 2.0
 */
public class ReactiveStringBasedCassandraQuery extends AbstractReactiveCassandraQuery {

	private static final String COUNT_AND_EXISTS = "Manually defined query for %s cannot be a count and exists query at the same time!";

	private final StringBasedQuery stringBasedQuery;

	private final boolean isCountQuery;

	private final boolean isExistsQuery;

	/**
	 * Create a new {@link ReactiveStringBasedCassandraQuery} for the given {@link CassandraQueryMethod},
	 * {@link ReactiveCassandraOperations}, {@link SpelExpressionParser}, and
	 * {@link QueryMethodEvaluationContextProvider}.
	 *
	 * @param queryMethod {@link ReactiveCassandraQueryMethod} on which this query is based.
	 * @param operations {@link ReactiveCassandraOperations} used to perform data access in Cassandra.
	 * @param expressionParser {@link SpelExpressionParser} used to parse expressions in the query.
	 * @param evaluationContextProvider {@link QueryMethodEvaluationContextProvider} used to access the potentially shared
	 *          {@link org.springframework.expression.spel.support.StandardEvaluationContext}.
	 * @see org.springframework.data.cassandra.repository.query.ReactiveCassandraQueryMethod
	 * @see org.springframework.data.cassandra.core.ReactiveCassandraOperations
	 */
	public ReactiveStringBasedCassandraQuery(ReactiveCassandraQueryMethod queryMethod,
			ReactiveCassandraOperations operations, SpelExpressionParser expressionParser,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		this(queryMethod.getRequiredAnnotatedQuery(), queryMethod, operations, expressionParser, evaluationContextProvider);
	}

	/**
	 * Create a new {@link ReactiveStringBasedCassandraQuery} for the given {@code query}, {@link CassandraQueryMethod},
	 * {@link ReactiveCassandraOperations}, {@link SpelExpressionParser}, and
	 * {@link QueryMethodEvaluationContextProvider}.
	 *
	 * @param method {@link ReactiveCassandraQueryMethod} on which this query is based.
	 * @param operations {@link ReactiveCassandraOperations} used to perform data access in Cassandra.
	 * @param expressionParser {@link SpelExpressionParser} used to parse expressions in the query.
	 * @param evaluationContextProvider {@link QueryMethodEvaluationContextProvider} used to access the potentially shared
	 *          {@link org.springframework.expression.spel.support.StandardEvaluationContext}.
	 * @see org.springframework.data.cassandra.repository.query.ReactiveCassandraQueryMethod
	 * @see org.springframework.data.cassandra.core.ReactiveCassandraOperations
	 */
	public ReactiveStringBasedCassandraQuery(String query, ReactiveCassandraQueryMethod method,
			ReactiveCassandraOperations operations, SpelExpressionParser expressionParser,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		super(method, operations);

		Assert.hasText(query, "Query must not be empty");

		this.stringBasedQuery = new StringBasedQuery(query,
				new ExpressionEvaluatingParameterBinder(expressionParser, evaluationContextProvider));

		if (method.hasAnnotatedQuery()) {

			Query queryAnnotation = method.getQueryAnnotation().orElse(null);

			this.isCountQuery = queryAnnotation.count();
			this.isExistsQuery = queryAnnotation.exists();

			if (ProjectionUtil.hasAmbiguousProjectionFlags(this.isCountQuery, this.isExistsQuery)) {
				throw new IllegalArgumentException(String.format(COUNT_AND_EXISTS, method));
			}
		} else {
			this.isCountQuery = false;
			this.isExistsQuery = false;
		}
	}

	protected StringBasedQuery getStringBasedQuery() {
		return this.stringBasedQuery;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.cassandra.repository.query.AbstractCassandraQuery#createQuery(org.springframework.data.cassandra.repository.query.CassandraParameterAccessor)
	 */
	@Override
	public SimpleStatement createQuery(CassandraParameterAccessor parameterAccessor) {
		return getQueryStatementCreator().select(getStringBasedQuery(), parameterAccessor);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.cassandra.repository.query.AbstractReactiveCassandraQuery#isCountQuery()
	 */
	@Override
	protected boolean isCountQuery() {
		return this.isCountQuery;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.cassandra.repository.query.AbstractReactiveCassandraQuery#isExistsQuery()
	 */
	@Override
	protected boolean isExistsQuery() {
		return this.isExistsQuery;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.cassandra.repository.query.AbstractReactiveCassandraQuery#isLimiting()
	 */
	@Override
	protected boolean isLimiting() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.cassandra.repository.query.AbstractCassandraQuery#isModifyingQuery()
	 */
	@Override
	protected boolean isModifyingQuery() {
		return false;
	}
}
