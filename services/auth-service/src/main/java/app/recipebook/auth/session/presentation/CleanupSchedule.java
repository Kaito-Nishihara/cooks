package app.recipebook.auth.session.presentation;

import app.recipebook.auth.session.application.CleanupExpiredRowsUseCase;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * 期限が切れた行を消す処理を、1 日に 1 回動かす入口(「共通設計: サービスの共通構成」の 4.9、決定の記録 0010)。
 * 動かす時刻は、設定 app.auth.cleanup-cron で決める。
 */
@ApplicationScoped
public class CleanupSchedule {

    @Inject
    CleanupExpiredRowsUseCase cleanup;

    // 前の回が終わっていなければ、その回は飛ばす。同じコンテナの中で重ねて動かさないためである
    @Scheduled(identity = "cleanup-expired-rows", cron = "{app.auth.cleanup-cron}", timeZone = "UTC",
            concurrentExecution = ConcurrentExecution.SKIP)
    void run() {
        cleanup.execute();
    }
}
