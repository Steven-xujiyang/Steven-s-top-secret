package com.quant.kernel.backtest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Falsifiable property #3:
 * Same RunSpec + same seed → byte-for-byte identical canonical result.
 *
 * <p>This test would fail if any component uses HashMap traversal,
 * UUID.randomUUID(), Instant.now(), or unseeded Random.
 */
class BacktestDeterminismTest {

    @Test
    void sameRunSpecProducesSameCanonicalArtifacts() {
        RunSpec spec = GoldenFixture.goldenRunSpec("determinism-test");
        BacktestEngine engine = new BacktestEngine();

        // Run 10 times
        String first = null;
        for (int i = 0; i < 10; i++) {
            // Reset and re-run
            BacktestEngine freshEngine = new BacktestEngine();
            BacktestResult result = freshEngine.run(spec);
            String canonical = CanonicalResult.render(result);

            if (first == null) {
                first = canonical;
            } else {
                assertThat(canonical)
                        .as("Run %d produced different canonical output", i + 1)
                        .isEqualTo(first);
            }
        }
    }
}
