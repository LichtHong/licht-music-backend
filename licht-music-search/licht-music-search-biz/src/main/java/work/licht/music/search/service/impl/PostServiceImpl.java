package work.licht.music.search.service.impl;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import work.licht.music.common.constant.DateConstants;
import work.licht.music.common.response.PageResponse;
import work.licht.music.common.response.Response;
import work.licht.music.common.util.DateUtils;
import work.licht.music.common.util.NumberUtils;
import work.licht.music.search.domain.mapper.SelectMapper;
import work.licht.music.search.dto.RebuildPostSearchDocReqDTO;
import work.licht.music.search.enums.PostPublishTimeRangeEnum;
import work.licht.music.search.enums.PostSortTypeEnum;
import work.licht.music.search.model.index.PostIndex;
import work.licht.music.search.model.vo.SearchPostReqVO;
import work.licht.music.search.model.vo.SearchPostRespVO;
import work.licht.music.search.service.PostService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// 帖子搜索业务
@Service
@Slf4j
public class PostServiceImpl implements PostService {

    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Resource
    private SelectMapper selectMapper;

    // 搜索帖子
    @Override
    public PageResponse<SearchPostRespVO> searchPost(SearchPostReqVO searchPostReqVO) {
        // 查询关键词
        String keyword = searchPostReqVO.getKeyword();
        // 当前页码
        Integer currentPage = searchPostReqVO.getCurrentPage();
        // 排序类型
        Integer sort = searchPostReqVO.getSort();
        // 发布时间范围
        Integer publishTimeRange = searchPostReqVO.getPublishTimeRange();
        // 构建 SearchRequest，指定要查询的索引
        SearchRequest searchRequest = new SearchRequest(PostIndex.NAME);
        // 创建查询构建器
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 创建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.multiMatchQuery(keyword).field(PostIndex.FIELD_POST_TITLE, 2.0f) // 手动设置帖子标题的权重值为 2.0
                .field(PostIndex.FIELD_POST_TOPIC) // 话题，权重默认为 1.0
        );
        // 按发布时间范围过滤
        PostPublishTimeRangeEnum postPublishTimeRangeEnum = PostPublishTimeRangeEnum.valueOf(publishTimeRange);
        if (Objects.nonNull(postPublishTimeRangeEnum)) {
            // 结束时间
            String endTime = LocalDateTime.now().format(DateConstants.DATE_FORMAT_Y_M_D_H_M_S);
            // 开始时间
            String startTime = null;
            switch (postPublishTimeRangeEnum) {
                case DAY -> startTime = DateUtils.localDateTime2String(LocalDateTime.now().minusDays(1));
                case WEEK -> startTime = DateUtils.localDateTime2String(LocalDateTime.now().minusWeeks(1));
                case HALF_YEAR -> startTime = DateUtils.localDateTime2String(LocalDateTime.now().minusMonths(6));
            }
            // 设置时间范围
            if (StringUtils.isNoneBlank(startTime)) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery(PostIndex.FIELD_POST_CREATE_TIME).gte(startTime).lte(endTime));
            }
        }
        // 排序
        PostSortTypeEnum postSortTypeEnum = PostSortTypeEnum.valueOf(sort);
        if (Objects.nonNull(postSortTypeEnum)) {
            switch (postSortTypeEnum) {
                // 按帖子发布时间降序
                case LATEST ->
                        sourceBuilder.sort(new FieldSortBuilder(PostIndex.FIELD_POST_CREATE_TIME).order(SortOrder.DESC));
                // 按帖子点赞量降序
                case MOST_LIKE ->
                        sourceBuilder.sort(new FieldSortBuilder(PostIndex.FIELD_POST_LIKE_TOTAL).order(SortOrder.DESC));
                // 按评论量降序
                case MOST_COMMENT ->
                        sourceBuilder.sort(new FieldSortBuilder(PostIndex.FIELD_POST_COMMENT_TOTAL).order(SortOrder.DESC));
                // 按收藏量降序
                case MOST_COLLECT ->
                        sourceBuilder.sort(new FieldSortBuilder(PostIndex.FIELD_POST_COLLECT_TOTAL).order(SortOrder.DESC));
            }
            // 设置查询
            sourceBuilder.query(boolQueryBuilder);
        } else {
            // 综合排序，自定义评分，并按 _score 评分降序
            sourceBuilder.sort(new FieldSortBuilder("_score").order(SortOrder.DESC));
            FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders = new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                    // function 1
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(new FieldValueFactorFunctionBuilder(PostIndex.FIELD_POST_LIKE_TOTAL).factor(0.5f).modifier(FieldValueFactorFunction.Modifier.SQRT).missing(0)),
                    // function 2
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(new FieldValueFactorFunctionBuilder(PostIndex.FIELD_POST_COLLECT_TOTAL).factor(0.3f).modifier(FieldValueFactorFunction.Modifier.SQRT).missing(0)),
                    // function 3
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(new FieldValueFactorFunctionBuilder(PostIndex.FIELD_POST_COMMENT_TOTAL).factor(0.2f).modifier(FieldValueFactorFunction.Modifier.SQRT).missing(0))};
            // 构建 function_score 查询
            FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(boolQueryBuilder, filterFunctionBuilders).scoreMode(FunctionScoreQuery.ScoreMode.SUM) // score_mode 为 sum
                    .boostMode(CombineFunction.SUM); // boost_mode 为 sum
            // 设置查询
            sourceBuilder.query(functionScoreQueryBuilder);
        }
        // 设置分页，from 和 size
        int pageSize = 10; // 每页展示数据量
        int from = (currentPage - 1) * pageSize; // 偏移量
        sourceBuilder.from(from);
        sourceBuilder.size(pageSize);
        // 将构建的查询条件设置到 SearchRequest 中
        searchRequest.source(sourceBuilder);
        // 返参 VO 集合
        List<SearchPostRespVO> searchPostRespVOList = null;
        // 总文档数，默认为 0
        long total = 0;
        try {
            log.info("==> SearchRequest: {}", searchRequest.source().toString());
            // 执行搜索
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            // 处理搜索结果
            total = searchResponse.getHits().getTotalHits().value;
            log.info("==> 命中文档总数, hits: {}", total);
            searchPostRespVOList = Lists.newArrayList();
            // 获取搜索命中的文档列表
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits) {
                log.info("==> 文档数据: {}", hit.getSourceAsString());
                // 获取文档的所有字段（以 Map 的形式返回）
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                // 提取特定字段值
                Long postId = (Long) sourceAsMap.get(PostIndex.FIELD_POST_ID);
                String cover = (String) sourceAsMap.get(PostIndex.FIELD_POST_COVER);
                String title = (String) sourceAsMap.get(PostIndex.FIELD_POST_TITLE);
                String avatar = (String) sourceAsMap.get(PostIndex.FIELD_POST_AVATAR);
                String nickname = (String) sourceAsMap.get(PostIndex.FIELD_POST_NICKNAME);
                Integer likeTotal = (Integer) sourceAsMap.get(PostIndex.FIELD_POST_LIKE_TOTAL);
                Integer commentTotal = (Integer) sourceAsMap.get(PostIndex.FIELD_POST_COMMENT_TOTAL);
                Integer collectTotal = (Integer) sourceAsMap.get(PostIndex.FIELD_POST_COLLECT_TOTAL);
                // 获取更新时间
                String updateTimeStr = (String) sourceAsMap.get(PostIndex.FIELD_POST_UPDATE_TIME);
                LocalDateTime updateTime = LocalDateTime.parse(updateTimeStr, DateConstants.DATE_FORMAT_Y_M_D_H_M_S);
                // 构建 VO 实体类
                SearchPostRespVO searchPostRespVO = SearchPostRespVO.builder()
                        .postId(postId)
                        .cover(cover)
                        .title(title)
                        .avatar(avatar)
                        .nickname(nickname)
                        .updateTime(DateUtils.formatRelativeTime(updateTime))
                        .likeTotal(NumberUtils.formatNumberString(likeTotal))
                        .commentTotal(NumberUtils.formatNumberString(commentTotal))
                        .collectTotal(NumberUtils.formatNumberString(collectTotal))
                        .build();
                searchPostRespVOList.add(searchPostRespVO);
            }
        } catch (IOException e) {
            log.error("==> 查询 Elasticsearch 异常: ", e);
        }
        return PageResponse.success(searchPostRespVOList, currentPage, total);
    }

    // 重建帖子文档
    @Override
    public Response<Long> rebuildSearchDocument(RebuildPostSearchDocReqDTO rebuildPostSearchDocReqDTO) {
        Long noteId = rebuildPostSearchDocReqDTO.getId();
        // 从数据库查询 Elasticsearch 索引数据
        List<Map<String, Object>> result = selectMapper.selectPostIndexData(noteId, null);
        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : result) {
            // 创建索引请求对象，指定索引名称
            IndexRequest indexRequest = new IndexRequest(PostIndex.NAME);
            // 设置文档的 ID，使用记录中的主键 “id” 字段值
            indexRequest.id((String.valueOf(recordMap.get(PostIndex.FIELD_POST_ID))));
            // 设置文档的内容，使用查询结果的记录数据
            indexRequest.source(recordMap);
            // 将数据写入 Elasticsearch 索引
            try {
                restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("==> 重建帖子文档失败: ", e);
            }
        }
        return Response.success();
    }

}