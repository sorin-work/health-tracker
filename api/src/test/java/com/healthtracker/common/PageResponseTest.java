package com.healthtracker.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PageResponseTest {

    private final List<Integer> numbers = IntStream.range(0, 25).boxed().toList();

    @Test
    void slicesThePage() {
        PageResponse<Integer> page = PageResponse.of(numbers, 1, 10);

        assertThat(page.items()).containsExactly(10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
        assertThat(page.total()).isEqualTo(25);
    }

    @Test
    void pastTheEndIsEmpty() {
        assertThat(PageResponse.of(numbers, 5, 10).items()).isEmpty();
    }

    @Test
    void clampsSillySizes() {
        assertThat(PageResponse.of(numbers, -1, 0).items()).containsExactly(0);
        assertThat(PageResponse.of(numbers, 0, 10_000).size()).isEqualTo(500);
    }
}
