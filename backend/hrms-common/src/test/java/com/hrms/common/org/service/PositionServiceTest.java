package com.hrms.common.org.service;

import com.hrms.common.exception.BizException;
import com.hrms.common.org.entity.Position;
import com.hrms.common.org.mapper.PositionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PositionService}.
 */
class PositionServiceTest {

    private PositionMapper positionMapper;
    private PositionService positionService;

    @BeforeEach
    void setUp() {
        positionMapper = mock(PositionMapper.class);
        positionService = new PositionService(positionMapper);
    }

    private Position position(Long id, int headcount, int occupied) {
        Position p = new Position();
        p.setId(id);
        p.setHeadcount(headcount);
        p.setOccupied(occupied);
        p.setName("pos-" + id);
        p.setDeptId(201L);
        p.setJobId(301L);
        return p;
    }

    @Test
    @DisplayName("incrOccupied rejects when headcount is full")
    void incrOccupiedRejectsWhenFull() {
        Position pos = position(1L, 1, 1);
        when(positionMapper.selectById(1L)).thenReturn(pos);

        assertThatThrownBy(() -> positionService.incrOccupied(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("headcount is full");
    }

    @Test
    @DisplayName("incrOccupied succeeds when under headcount")
    void incrOccupiedSucceeds() {
        Position pos = position(1L, 3, 1);
        when(positionMapper.selectById(1L)).thenReturn(pos);

        Position result = positionService.incrOccupied(1L);

        assertThat(result.getOccupied()).isEqualTo(2);
        verify(positionMapper).updateById(any(Position.class));
    }

    @Test
    @DisplayName("decrOccupied rejects when occupied is already 0")
    void decrOccupiedRejectsWhenZero() {
        Position pos = position(1L, 3, 0);
        when(positionMapper.selectById(1L)).thenReturn(pos);

        assertThatThrownBy(() -> positionService.decrOccupied(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("already 0");
    }

    @Test
    @DisplayName("decrOccupied succeeds when occupied > 0")
    void decrOccupiedSucceeds() {
        Position pos = position(1L, 3, 2);
        when(positionMapper.selectById(1L)).thenReturn(pos);

        Position result = positionService.decrOccupied(1L);

        assertThat(result.getOccupied()).isEqualTo(1);
        verify(positionMapper).updateById(any(Position.class));
    }

    @Test
    @DisplayName("delete rejects when position has active employees")
    void deleteRejectsWithActiveEmployees() {
        Position pos = position(1L, 3, 2);
        when(positionMapper.selectById(1L)).thenReturn(pos);

        assertThatThrownBy(() -> positionService.delete(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("active employees");
    }

    @Test
    @DisplayName("delete succeeds when position has no active employees")
    void deleteSucceedsWhenEmpty() {
        Position pos = position(1L, 3, 0);
        when(positionMapper.selectById(1L)).thenReturn(pos);

        positionService.delete(1L);

        verify(positionMapper).deleteById(1L);
    }
}
