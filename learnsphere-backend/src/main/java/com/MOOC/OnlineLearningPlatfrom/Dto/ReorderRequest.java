package com.MOOC.OnlineLearningPlatfrom.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {

    @JsonAlias({"newPosition", "targetPosition"})
    private Integer position;

    @JsonAlias({"ids", "orderedModuleIds", "orderedContentIds"})
    private List<Long> orderedIds;
}
