package com.example.smartmessaging.service.repository;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.vo.ChannelStatVO;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.ClickStatVO;
import com.example.smartmessaging.dto.vo.CustomerChannelConsentSummaryVO;
import com.example.smartmessaging.dto.vo.CustomerStatVO;
import com.example.smartmessaging.dto.vo.MessageStatByDegreeVO;
import com.example.smartmessaging.dto.vo.MessageStatVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StatMapper {

    List<ChannelVO> selectActiveChannels();

    List<MessageStatVO> selectMessageStats(StatSearchRequest request);

    List<MessageStatByDegreeVO> selectDeliveryMessageStatByDegrees(StatSearchRequest request);

    List<ChannelStatVO> selectChannelStats(StatSearchRequest request);

    List<CustomerStatVO> selectCustomerStats(StatSearchRequest request);

    List<CustomerChannelConsentSummaryVO> selectCustomerChannelConsents(StatSearchRequest request);

    List<ClickStatVO> selectPerformanceClickStats(StatSearchRequest request);

    List<ClickStatVO> selectPerformanceConversionStats(StatSearchRequest request);
}
