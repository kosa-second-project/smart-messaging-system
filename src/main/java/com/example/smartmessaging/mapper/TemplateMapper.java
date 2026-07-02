package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.request.TemplateSearchRequest;
import com.example.smartmessaging.dto.response.TemplateChannelResponse;
import com.example.smartmessaging.dto.response.TemplateFilterOptionResponse;
import com.example.smartmessaging.dto.response.TemplateResponse;
import com.example.smartmessaging.dto.response.TemplateStatResponse;
import com.example.smartmessaging.dto.vo.TemplateChannelVO;
import com.example.smartmessaging.dto.vo.TemplateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TemplateMapper {

    List<TemplateResponse> selectTemplateList(TemplateSearchRequest request);

    int selectTemplateCount(TemplateSearchRequest request);

    TemplateResponse selectTemplateDetail(@Param("userId") Long userId, @Param("templateId") Long templateId);

    List<TemplateChannelResponse> selectChannelsByTemplateIds(@Param("templateIds") List<Long> templateIds);

    List<TemplateChannelResponse> selectAllChannels();

    List<TemplateFilterOptionResponse> selectCategoryOptions(@Param("userId") Long userId);

    List<TemplateFilterOptionResponse> selectPurposeOptions(@Param("userId") Long userId);

    List<TemplateFilterOptionResponse> selectTagOptions();

    TemplateStatResponse selectTemplateStats(@Param("userId") Long userId);

    int insertTemplate(TemplateVO template);

    int insertTemplateChannel(TemplateChannelVO templateChannel);
}
