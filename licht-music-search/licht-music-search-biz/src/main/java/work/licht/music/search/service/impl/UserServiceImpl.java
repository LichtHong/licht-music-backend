package work.licht.music.search.service.impl;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import work.licht.music.common.response.PageResponse;
import work.licht.music.common.response.Response;
import work.licht.music.common.util.NumberUtils;
import work.licht.music.search.domain.mapper.SelectMapper;
import work.licht.music.search.dto.RebuildUserSearchDocReqDTO;
import work.licht.music.search.model.index.UserIndex;
import work.licht.music.search.model.vo.SearchUserReqVO;
import work.licht.music.search.model.vo.SearchUserRespVO;
import work.licht.music.search.service.UserService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// 用户搜索业务
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private RestHighLevelClient restHighLevelClient;
    @Resource
    private SelectMapper selectMapper;

    // 搜索用户
    @Override
    public PageResponse<SearchUserRespVO> searchUser(SearchUserReqVO searchUserReqVO) {
        // 查询关键词
        String keyword = searchUserReqVO.getKeyword();
        // 当前页码
        Integer currentPage = searchUserReqVO.getCurrentPage();
        // 构建 SearchRequest，指定索引
        SearchRequest searchRequest = new SearchRequest(UserIndex.NAME);
        // 构建查询内容
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 构建 multi_match 查询，查询 nickname 和 username 字段
        sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, UserIndex.FIELD_USER_NICKNAME, UserIndex.FIELD_USER_USERNAME));
        // 排序，按 follower_total 降序
        SortBuilder<?> sortBuilder = new FieldSortBuilder(UserIndex.FIELD_USER_FOLLOWER_TOTAL).order(SortOrder.DESC);
        sourceBuilder.sort(sortBuilder);
        // 设置分页，from 和 size
        int pageSize = 10; // 每页展示数据量
        int from = (currentPage - 1) * pageSize; // 偏移量
        sourceBuilder.from(from);
        sourceBuilder.size(pageSize);
        // 将构建的查询条件设置到 SearchRequest 中
        searchRequest.source(sourceBuilder);
        // 返参 VO 集合
        List<SearchUserRespVO> searchUserRespVOList = null;
        // 总文档数，默认为 0
        long total = 0;
        try {
            log.info("==> SearchRequest: {}", searchRequest);
            // 执行查询请求
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            // 处理搜索结果
            total = searchResponse.getHits().getTotalHits().value;
            log.info("==> 命中文档总数, hits: {}", total);
            searchUserRespVOList = Lists.newArrayList();
            // 获取搜索命中的文档列表
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits) {
                log.info("==> 文档数据: {}", hit.getSourceAsString());
                // 获取文档的所有字段（以 Map 的形式返回）
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                // 提取特定字段值
                Long userId = ((Number) sourceAsMap.get(UserIndex.FIELD_USER_ID)).longValue();
                String username = (String) sourceAsMap.get(UserIndex.FIELD_USER_USERNAME);
                String nickname = (String) sourceAsMap.get(UserIndex.FIELD_USER_NICKNAME);
                String avatar = (String) sourceAsMap.get(UserIndex.FIELD_USER_AVATAR);
                Integer postTotal = (Integer) sourceAsMap.get(UserIndex.FIELD_USER_PUBLISH_TOTAL);
                Integer followerTotal = (Integer) sourceAsMap.get(UserIndex.FIELD_USER_FOLLOWER_TOTAL);
                // 构建 VO 实体类
                SearchUserRespVO searchUserRespVO = SearchUserRespVO.builder()
                        .userId(userId)
                        .username(username)
                        .nickname(nickname)
                        .avatar(avatar)
                        .postTotal(NumberUtils.formatNumberString(postTotal))
                        .followerTotal(NumberUtils.formatNumberString(followerTotal))
                        .build();
                searchUserRespVOList.add(searchUserRespVO);
            }
        } catch (Exception e) {
            log.error("==> 查询 Elasticsearch 异常: ", e);
        }
        return PageResponse.success(searchUserRespVOList, currentPage, total);
    }

    // 重建用户文档
    @Override
    public Response<Long> rebuildDocument(RebuildUserSearchDocReqDTO rebuildUserDocumentReqDTO) {
        Long userId = rebuildUserDocumentReqDTO.getId();
        // 从数据库查询 Elasticsearch 索引数据
        List<Map<String, Object>> result = selectMapper.selectUserIndexData(userId);
        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : result) {
            // 创建索引请求对象，指定索引名称
            IndexRequest indexRequest = new IndexRequest(UserIndex.NAME);
            // 设置文档的 ID，使用记录中的主键 “id” 字段值
            indexRequest.id((String.valueOf(recordMap.get(UserIndex.FIELD_USER_ID))));
            // 设置文档的内容，使用查询结果的记录数据
            indexRequest.source(recordMap);
            // 将数据写入 Elasticsearch 索引
            try {
                restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("==> 重建用户文档异常: ", e);
            }
        }
        return Response.success();
    }

}