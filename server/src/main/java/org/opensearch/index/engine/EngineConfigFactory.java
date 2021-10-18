/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.engine;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.similarities.Similarity;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.codec.CodecService;
import org.opensearch.index.seqno.RetentionLeases;
import org.opensearch.index.shard.ShardId;
import org.opensearch.index.store.Store;
import org.opensearch.index.translog.TranslogConfig;
import org.opensearch.indices.breaker.CircuitBreakerService;
import org.opensearch.plugins.EnginePlugin;
import org.opensearch.plugins.PluginsService;
import org.opensearch.threadpool.ThreadPool;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class EngineConfigFactory {
    private final Optional<CodecService> codecService;
    private final Optional<TranslogConfig> translogConfig;

    public EngineConfigFactory(IndexSettings idxSettings) {
        this(Collections.emptyList(), idxSettings);
    }

    public EngineConfigFactory(PluginsService pluginsService, IndexSettings idxSettings) {
        this(pluginsService.filterPlugins(EnginePlugin.class), idxSettings);
    }

    EngineConfigFactory(Collection<EnginePlugin> enginePlugins, IndexSettings idxSettings) {
        Optional<TranslogConfig> translogConfig = Optional.empty();
        String translogConfigOverridingPlugin = null;
        Optional<CodecService> codecService = Optional.empty();
        String codecServiceOverridingPlugin = null;
        for (EnginePlugin enginePlugin : enginePlugins) {
            // get overriding translog config from EnginePlugin
            if (translogConfig.isPresent() == false) {
                translogConfig = enginePlugin.getCustomTranslogConfig(idxSettings);
                translogConfigOverridingPlugin = enginePlugin.getClass().getName();
            } else {
                throw new IllegalStateException("existing translog configuration already overridden in: " + translogConfigOverridingPlugin
                    + " attempting to override again by: " + enginePlugin.getClass().getName());
            }

            // get overriding codec service from EnginePlugin
            if (codecService.isPresent() == false) {
                codecService = enginePlugin.getCustomCodecService(idxSettings);
                codecServiceOverridingPlugin = enginePlugin.getClass().getName();
            } else {
                throw new IllegalStateException("existing codec service already overridden in: " + codecServiceOverridingPlugin
                    + " attempting to override again by: " + enginePlugin.getClass().getName());
            }
        }
        this.translogConfig = translogConfig;
        this.codecService = codecService;
    }

    public EngineConfig newEngineConfig(ShardId shardId, ThreadPool threadPool,
                                        IndexSettings indexSettings, Engine.Warmer warmer, Store store,
                                        MergePolicy mergePolicy, Analyzer analyzer,
                                        Similarity similarity, CodecService codecService, Engine.EventListener eventListener,
                                        QueryCache queryCache, QueryCachingPolicy queryCachingPolicy, TranslogConfig translogConfig,
                                        TimeValue flushMergesAfter,
                                        List<ReferenceManager.RefreshListener> externalRefreshListener,
                                        List<ReferenceManager.RefreshListener> internalRefreshListener, Sort indexSort,
                                        CircuitBreakerService circuitBreakerService, LongSupplier globalCheckpointSupplier,
                                        Supplier<RetentionLeases> retentionLeasesSupplier,
                                        LongSupplier primaryTermSupplier,
                                        EngineConfig.TombstoneDocSupplier tombstoneDocSupplier) {

        return new EngineConfig(shardId, threadPool, indexSettings, warmer, store, mergePolicy, analyzer, similarity,
            this.codecService.isPresent() == true ? this.codecService.get() : codecService, eventListener, queryCache,
            queryCachingPolicy, this.translogConfig.isPresent() == true ? this.translogConfig.get() : translogConfig,
            flushMergesAfter, externalRefreshListener, internalRefreshListener, indexSort, circuitBreakerService,
            globalCheckpointSupplier, retentionLeasesSupplier, primaryTermSupplier, tombstoneDocSupplier);
    }
}
