package cn.edu.kpi.application.indicator;

import cn.edu.kpi.domain.model.IndicatorVersion;

import java.time.Instant;
import java.util.Optional;

/** Published indicators are immutable calculation inputs. */
public interface IndicatorVersionRepository {
    Optional<IndicatorVersion> findPublished(String versionCode);

    void publish(IndicatorVersion version, String formulaVersion, int scoreScale, Instant publishedAt);
}
