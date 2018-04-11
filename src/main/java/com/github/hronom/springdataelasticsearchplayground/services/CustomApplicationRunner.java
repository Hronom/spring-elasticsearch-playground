package com.github.hronom.springdataelasticsearchplayground.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.Date;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Service
public class CustomApplicationRunner implements ApplicationRunner {
    private final Log logger = LogFactory.getLog(getClass());

    private final TransportClient transportClient;

    @Autowired
    public CustomApplicationRunner(TransportClient transportClient) {
        this.transportClient = transportClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        IndicesExistsResponse indicesExistsResponse =
            transportClient.admin().indices().exists(new IndicesExistsRequest("twitter")).get();
        if (indicesExistsResponse.isExists()) {
            transportClient.admin().indices().delete(new DeleteIndexRequest("twitter")).get();
        }

        XContentBuilder xContentBuilder =
            jsonBuilder()
                .startObject()
                    .field("tweet")
                        .startObject()
                            .field("properties")
                                .startObject()
                                    .field("message")
                                        .startObject()
                                            .field("type", "text")
                                        .endObject()
                                    .field("tags")
                                        .startObject()
                                        .field("type", "keyword")
                                        .endObject()
                                .endObject()
                        .endObject()
                .endObject();
        transportClient
            .admin()
            .indices()
            .prepareCreate("twitter")
            .addMapping("tweet", xContentBuilder)
            .get();


        for (int i = 0; i < 10; i++) {
            IndexRequest indexRequest =
                transportClient
                    .prepareIndex("twitter", "tweet", String.valueOf(i))
                    .setSource(
                        jsonBuilder()
                            .startObject()
                            .field("message", "Hello world")
                            .field("postDate", new Date())
                            .field("tags", new String[] {"tag" + i, "tag" + (i - 1)})
                            .endObject()
                    )
                    .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                    .request();
            transportClient.index(indexRequest).get();
        }

        //transportClient.admin().indices().refresh(new RefreshRequest("twitter"));

        /*AggregationBuilder aggregation =
            AggregationBuilders
                .filters(
                    "tags_filter",
                    QueryBuilders.termQuery("tags", "tag2")
                );
        SearchResponse sr =
            transportClient
                .prepareSearch()
                .addAggregation(aggregation)
                .get();
        Filters agg = sr.getAggregations().get("tags_filter");
        // For each entry
        for (Filters.Bucket entry : agg.getBuckets()) {
            String key = entry.getKeyAsString();            // bucket key
            long docCount = entry.getDocCount();            // Doc count
            logger.info(String.format("key [{}], doc_count [{}]" + key, docCount));
        }*/

        SearchResponse sr =
            transportClient
                .prepareSearch("twitter")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTypes("tweet")
                .setQuery(
                    QueryBuilders
                        .boolQuery()
                        .must(QueryBuilders.termQuery("tags", "tag1"))
                        .must(QueryBuilders.termQuery("tags", "tag2"))
                )
                .get();
        for (SearchHit searchHit : sr.getHits().getHits()) {
            logger.info(searchHit.getSourceAsString());
        }
    }
}
