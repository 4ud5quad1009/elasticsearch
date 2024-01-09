/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.expression.function.scalar.multivalue;

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.compute.ann.MvEvaluator;
import org.elasticsearch.compute.data.BooleanBlock;
import org.elasticsearch.compute.data.BytesRefBlock;
import org.elasticsearch.compute.data.DoubleBlock;
import org.elasticsearch.compute.data.IntBlock;
import org.elasticsearch.compute.data.LongBlock;
import org.elasticsearch.compute.operator.EvalOperator;
import org.elasticsearch.compute.operator.EvalOperator.ExpressionEvaluator;
import org.elasticsearch.xpack.esql.EsqlIllegalArgumentException;
import org.elasticsearch.xpack.esql.expression.function.FunctionInfo;
import org.elasticsearch.xpack.esql.expression.function.Param;
import org.elasticsearch.xpack.esql.planner.PlannerUtils;
import org.elasticsearch.xpack.esql.type.EsqlDataTypes;
import org.elasticsearch.xpack.ql.expression.Expression;
import org.elasticsearch.xpack.ql.tree.NodeInfo;
import org.elasticsearch.xpack.ql.tree.Source;

import java.util.List;

import static org.elasticsearch.xpack.ql.expression.TypeResolutions.isType;

/**
 * Reduce a multivalued field to a single valued field containing the minimum value.
 */
public class MvFirst extends AbstractMultivalueFunction {
    @FunctionInfo(returnType = "?", description = "Reduce a multivalued field to a single valued field containing the first value.")
    public MvFirst(
        Source source,
        @Param(
            name = "v",
            type = {
                "unsigned_long",
                "date",
                "boolean",
                "double",
                "ip",
                "text",
                "integer",
                "keyword",
                "version",
                "long",
                "geo_point",
                "cartesian_point" }
        ) Expression field
    ) {
        super(source, field);
    }

    @Override
    protected TypeResolution resolveFieldType() {
        return isType(field(), EsqlDataTypes::isRepresentable, sourceText(), null, "representable");
    }

    @Override
    protected ExpressionEvaluator.Factory evaluator(ExpressionEvaluator.Factory fieldEval) {
        return switch (PlannerUtils.toElementType(field().dataType())) {
            case BOOLEAN -> new MvFirstBooleanEvaluator.Factory(fieldEval);
            case BYTES_REF -> new MvFirstBytesRefEvaluator.Factory(fieldEval);
            case DOUBLE -> new MvFirstDoubleEvaluator.Factory(fieldEval);
            case INT -> new MvFirstIntEvaluator.Factory(fieldEval);
            case LONG -> new MvFirstLongEvaluator.Factory(fieldEval);
            case NULL -> EvalOperator.CONSTANT_NULL_FACTORY;
            default -> throw EsqlIllegalArgumentException.illegalDataType(field.dataType());
        };
    }

    @Override
    public Expression replaceChildren(List<Expression> newChildren) {
        return new MvFirst(source(), newChildren.get(0));
    }

    @Override
    protected NodeInfo<? extends Expression> info() {
        return NodeInfo.create(this, MvFirst::new, field());
    }

    @MvEvaluator(extraName = "Boolean")
    static boolean process(BooleanBlock block, int start, int end) {
        return block.getBoolean(start);
    }

    @MvEvaluator(extraName = "Long")
    static long process(LongBlock block, int start, int end) {
        return block.getLong(start);
    }

    @MvEvaluator(extraName = "Int")
    static int process(IntBlock block, int start, int end) {
        return block.getInt(start);
    }

    @MvEvaluator(extraName = "Double")
    static double process(DoubleBlock block, int start, int end) {
        return block.getDouble(start);
    }

    @MvEvaluator(extraName = "BytesRef")
    static BytesRef process(BytesRefBlock block, int start, int end, BytesRef scratch) {
        return block.getBytesRef(start, scratch);
    }
}
