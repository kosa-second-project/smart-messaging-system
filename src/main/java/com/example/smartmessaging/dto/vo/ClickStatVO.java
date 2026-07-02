package com.example.smartmessaging.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickStatVO extends BaseVO {
    private Long id;
    private LocalDate statDate;
    private Long channelId;
    private Integer hour00;
    private Integer hour01;
    private Integer hour02;
    private Integer hour03;
    private Integer hour04;
    private Integer hour05;
    private Integer hour06;
    private Integer hour07;
    private Integer hour08;
    private Integer hour09;
    private Integer hour10;
    private Integer hour11;
    private Integer hour12;
    private Integer hour13;
    private Integer hour14;
    private Integer hour15;
    private Integer hour16;
    private Integer hour17;
    private Integer hour18;
    private Integer hour19;
    private Integer hour20;
    private Integer hour21;
    private Integer hour22;
    private Integer hour23;
}
