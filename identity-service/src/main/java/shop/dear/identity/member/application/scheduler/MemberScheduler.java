package shop.dear.identity.member.application.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.IntSupplier;

@Slf4j
@Component
public class MemberScheduler {

	//대상이 줄지 않는 비정상 상황에서 무한 루프를 막기 위한 안전장치
	private static final int MAX_CHUNK_COUNT = 1_000;

	private final AccountArchiveProcessor accountArchiveProcessor;
	private final int chunkSize;

	public MemberScheduler(
		final AccountArchiveProcessor accountArchiveProcessor,
		@Value("${member.scheduler.chunk-size:500}") final int chunkSize
	) {
		this.accountArchiveProcessor = accountArchiveProcessor;
		this.chunkSize = chunkSize;
	}

	//계좌정보 이관
	public long archiveAccounts(final LocalDateTime baseDate) {

		return repeatByChunk(
			"계좌정보 이관",
			() -> accountArchiveProcessor.archiveAccountsByChunk(baseDate, chunkSize)
		);
	}

	//계좌정보 폐기
	public long deleteExpiredAccounts() {

		LocalDateTime baseDate = LocalDateTime.now();

		return repeatByChunk(
			"계좌정보 폐기",
			() -> accountArchiveProcessor.deleteAccountsByChunk(baseDate, chunkSize)
		);
	}

	/**
	 * 처리 건수가 청크 크기보다 작아지면 남은 대상이 없다는 뜻이므로 종료한다.
	 * 청크마다 트랜잭션이 끊기므로 앞선 청크의 결과는 이미 커밋된 상태다.
	 */
	private long repeatByChunk(final String taskName, final IntSupplier chunkTask) {

		long total = 0;

		for (int i = 0; i < MAX_CHUNK_COUNT; i++) {

			int processed = chunkTask.getAsInt();
			total += processed;

			if (processed < chunkSize) {
				return total;
			}
		}

		log.warn("[Scheduler] {} - 최대 청크 수({}) 도달, 남은 대상은 다음 실행에서 처리됩니다. 누적 {}건",
			taskName, MAX_CHUNK_COUNT, total);

		return total;
	}
}
