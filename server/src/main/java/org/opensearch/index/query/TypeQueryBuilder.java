/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.index.query;

import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.opensearch.Version;
import org.opensearch.common.ParseField;
import org.opensearch.common.ParsingException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.logging.DeprecationLogger;
import org.opensearch.common.lucene.search.Queries;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.index.mapper.DocumentMapper;

import java.io.IOException;
import java.util.Objects;

public class TypeQueryBuilder extends AbstractQueryBuilder<TypeQueryBuilder> {
    public static final String NAME = "type";

    private static final ParseField VALUE_FIELD = new ParseField("value");
    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(TypeQueryBuilder.class);
    static final String TYPES_DEPRECATION_MESSAGE = "[types removal] Type queries are deprecated, " +
        "prefer to filter on a field instead.";

    private final String type;

    public TypeQueryBuilder(String type) {
        if (type == null) {
            throw new IllegalArgumentException("[type] cannot be null");
        }
        this.type = type;
    }

    /**
     * Read from a stream.
     */
    public TypeQueryBuilder(StreamInput in) throws IOException {
        super(in);
        if (in.getVersion().onOrAfter(Version.V_6_3_0)) {
            type = in.readString();
        } else {
            type = in.readBytesRef().utf8ToString();
        }
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        if (out.getVersion().onOrAfter(Version.V_6_3_0)) {
            out.writeString(type);
        } else {
            out.writeBytesRef(new BytesRef(type));
        }
    }

    public String type() {
        return type;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        builder.field(VALUE_FIELD.getPreferredName(), type);
        printBoostAndQueryName(builder);
        builder.endObject();
    }

    public static TypeQueryBuilder fromXContent(XContentParser parser) throws IOException {
        String type = null;
        String queryName = null;
        float boost = AbstractQueryBuilder.DEFAULT_BOOST;
        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                if (AbstractQueryBuilder.NAME_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                    queryName = parser.text();
                } else if (AbstractQueryBuilder.BOOST_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                    boost = parser.floatValue();
                } else if (VALUE_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                    type = parser.text();
                } else {
                    throw new ParsingException(parser.getTokenLocation(),
                            "[" + TypeQueryBuilder.NAME + "] filter doesn't support [" + currentFieldName + "]");
                }
            } else {
                throw new ParsingException(parser.getTokenLocation(),
                        "[" + TypeQueryBuilder.NAME + "] filter doesn't support [" + currentFieldName + "]");
            }
        }

        if (type == null) {
            throw new ParsingException(parser.getTokenLocation(),
                    "[" + TypeQueryBuilder.NAME + "] filter needs to be provided with a value for the type");
        }
        return new TypeQueryBuilder(type)
                .boost(boost)
                .queryName(queryName);
    }


    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    protected Query doToQuery(QueryShardContext context) throws IOException {
        deprecationLogger.deprecate("type_query", TYPES_DEPRECATION_MESSAGE);
        //LUCENE 4 UPGRADE document mapper should use bytesref as well?
        DocumentMapper documentMapper = context.getMapperService().documentMapper(type);
        if (documentMapper == null) {
            // no type means no documents
            return new MatchNoDocsQuery();
        } else {
            return Queries.newNonNestedFilter(context.indexVersionCreated());
        }
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(type);
    }

    @Override
    protected boolean doEquals(TypeQueryBuilder other) {
        return Objects.equals(type, other.type);
    }
}
