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

package org.elasticsearch.search.aggregations.bucket.histogram;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.aggregations.InternalMultiBucketAggregationTestCase;
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.test.InternalAggregationTestCase;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.elasticsearch.common.unit.TimeValue.timeValueHours;
import static org.elasticsearch.common.unit.TimeValue.timeValueMinutes;
import static org.elasticsearch.common.unit.TimeValue.timeValueSeconds;

public class InternalDateHistogramTests extends InternalMultiBucketAggregationTestCase<InternalDateHistogram> {

    private boolean keyed;
    private DocValueFormat format;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        keyed = randomBoolean();
        format = randomNumericDocValueFormat();
    }

    @Override
    protected InternalDateHistogram createTestInstance(String name,
                                                       List<PipelineAggregator> pipelineAggregators,
                                                       Map<String, Object> metaData,
                                                       InternalAggregations aggregations) {
        int nbBuckets = randomNumberOfBuckets();
        List<InternalDateHistogram.Bucket> buckets = new ArrayList<>(nbBuckets);
        long startingDate = System.currentTimeMillis();

        long interval = randomIntBetween(1, 3);
        long intervalMillis = randomFrom(timeValueSeconds(interval), timeValueMinutes(interval), timeValueHours(interval)).getMillis();

        for (int i = 0; i < nbBuckets; i++) {
            long key = startingDate + (intervalMillis * i);
            buckets.add(i, new InternalDateHistogram.Bucket(key, randomIntBetween(1, 100), keyed, format, aggregations));
        }

        BucketOrder order = randomFrom(BucketOrder.key(true), BucketOrder.key(false));
        return new InternalDateHistogram(name, buckets, order, 1, 0L, null, format, keyed, pipelineAggregators, metaData);
    }

    @Override
    protected void assertReduced(InternalDateHistogram reduced, List<InternalDateHistogram> inputs) {
        Map<Long, Long> expectedCounts = new TreeMap<>();
        for (Histogram histogram : inputs) {
            for (Histogram.Bucket bucket : histogram.getBuckets()) {
                expectedCounts.compute(((DateTime) bucket.getKey()).getMillis(),
                        (key, oldValue) -> (oldValue == null ? 0 : oldValue) + bucket.getDocCount());
            }
        }
        Map<Long, Long> actualCounts = new TreeMap<>();
        for (Histogram.Bucket bucket : reduced.getBuckets()) {
            actualCounts.compute(((DateTime) bucket.getKey()).getMillis(),
                    (key, oldValue) -> (oldValue == null ? 0 : oldValue) + bucket.getDocCount());
        }
        assertEquals(expectedCounts, actualCounts);
    }

    @Override
    protected Writeable.Reader<InternalDateHistogram> instanceReader() {
        return InternalDateHistogram::new;
    }

    @Override
    protected Class<? extends ParsedMultiBucketAggregation> implementationClass() {
        return ParsedDateHistogram.class;
    }
}
