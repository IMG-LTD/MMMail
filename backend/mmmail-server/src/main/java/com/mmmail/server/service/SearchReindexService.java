package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SearchIndexMapper;
import com.mmmail.server.mapper.SearchIndexStagingMapper;
import com.mmmail.server.mapper.SearchReindexJobMapper;
import com.mmmail.server.model.entity.SearchIndex;
import com.mmmail.server.model.entity.SearchIndexStaging;
import com.mmmail.server.model.entity.SearchReindexJob;
import com.mmmail.server.model.vo.SearchReindexJobVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class SearchReindexService {
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_SUCCEEDED = "succeeded";

    private final SearchIndexMapper searchIndexMapper;
    private final SearchIndexStagingMapper stagingMapper;
    private final SearchReindexJobMapper jobMapper;
    private final SearchIndexCollectorService collectorService;

    public SearchReindexService(
            SearchIndexMapper searchIndexMapper,
            SearchIndexStagingMapper stagingMapper,
            SearchReindexJobMapper jobMapper,
            SearchIndexCollectorService collectorService
    ) {
        this.searchIndexMapper = searchIndexMapper;
        this.stagingMapper = stagingMapper;
        this.jobMapper = jobMapper;
        this.collectorService = collectorService;
    }

    @Transactional
    public SearchReindexJobVo reindex(String rawModuleType) {
        String moduleType = SearchModules.normalizeOne(rawModuleType);
        LinkedHashSet<String> modules = SearchModules.parse(moduleType);
        SearchReindexJob job = createJob(moduleType);
        List<SearchIndex> documents = collectorService.collect(modules);
        stageDocuments(job.getId(), documents);
        replaceIndex(job, modules, documents.size());
        return toVo(job);
    }

    public SearchReindexJobVo get(String jobId) {
        SearchReindexJob job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.SEARCH_REINDEX_JOB_NOT_FOUND);
        }
        return toVo(job);
    }

    private SearchReindexJob createJob(String moduleType) {
        LocalDateTime now = LocalDateTime.now();
        SearchReindexJob job = new SearchReindexJob();
        job.setId("sr_" + IdWorker.getIdStr());
        job.setModuleType(moduleType);
        job.setStatus(STATUS_RUNNING);
        job.setProcessed(0);
        job.setTotal(0);
        job.setErrors(0);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobMapper.insert(job);
        return job;
    }

    private void stageDocuments(String jobId, List<SearchIndex> documents) {
        stagingMapper.delete(new LambdaQueryWrapper<SearchIndexStaging>()
                .eq(SearchIndexStaging::getJobId, jobId));
        for (SearchIndex document : documents) {
            stagingMapper.insert(toStage(jobId, document));
        }
    }

    private void replaceIndex(SearchReindexJob job, LinkedHashSet<String> modules, int total) {
        searchIndexMapper.deleteByModuleTypes(modules);
        stagingMapper.selectList(new LambdaQueryWrapper<SearchIndexStaging>()
                        .eq(SearchIndexStaging::getJobId, job.getId()))
                .forEach(stage -> searchIndexMapper.insert(fromStage(stage)));
        completeJob(job, total);
    }

    private void completeJob(SearchReindexJob job, int total) {
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(STATUS_SUCCEEDED);
        job.setTotal(total);
        job.setProcessed(total);
        job.setErrors(0);
        job.setUpdatedAt(now);
        job.setCompletedAt(now);
        jobMapper.updateById(job);
    }

    private SearchIndexStaging toStage(String jobId, SearchIndex document) {
        SearchIndexStaging stage = new SearchIndexStaging();
        stage.setJobId(jobId);
        copyToStage(document, stage);
        return stage;
    }

    private void copyToStage(SearchIndex source, SearchIndexStaging target) {
        target.setModuleType(source.getModuleType());
        target.setResourceId(source.getResourceId());
        target.setOrgId(source.getOrgId());
        target.setOwnerUserId(source.getOwnerUserId());
        target.setAclUserIds(source.getAclUserIds());
        target.setTitle(source.getTitle());
        target.setBody(source.getBody());
        target.setRoutePath(source.getRoutePath());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setCreatedAt(source.getCreatedAt());
        target.setDeleted(source.getDeleted());
    }

    private SearchIndex fromStage(SearchIndexStaging stage) {
        SearchIndex row = new SearchIndex();
        row.setModuleType(stage.getModuleType());
        row.setResourceId(stage.getResourceId());
        row.setOrgId(stage.getOrgId());
        row.setOwnerUserId(stage.getOwnerUserId());
        row.setAclUserIds(stage.getAclUserIds());
        row.setTitle(stage.getTitle());
        row.setBody(stage.getBody());
        row.setRoutePath(stage.getRoutePath());
        row.setUpdatedAt(stage.getUpdatedAt());
        row.setCreatedAt(stage.getCreatedAt());
        row.setDeleted(stage.getDeleted());
        return row;
    }

    private SearchReindexJobVo toVo(SearchReindexJob job) {
        return new SearchReindexJobVo(
                job.getId(),
                job.getModuleType(),
                job.getStatus(),
                value(job.getProcessed()),
                value(job.getTotal()),
                value(job.getErrors()),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
