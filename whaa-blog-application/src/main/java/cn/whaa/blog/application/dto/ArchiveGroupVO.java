package cn.whaa.blog.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArchiveGroupVO {
    private int year;
    private List<ArchiveItemVO> items;
}
