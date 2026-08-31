package shop.dear.recommendation.infrastructure.ai;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentNormalizer implements DocumentTransformer {

	@Override
	public List<Document> apply(List<Document> documents) {
		return documents.stream()
			.map(this::normalizeDocument)
			.collect(Collectors.toList());
	}

	private Document normalizeDocument(Document doc) {
		String rawText = doc.getText();

		// 1. 텍스트 정제 및 공백 표준화 (줄바꿈, 탭, 연속 공백을 단일 공백으로 처리)
		String normalizedText = rawText.replaceAll("\\s+", " ").trim();

		// 2. 특수문자 정제
		normalizedText = normalizedText
			.replaceAll("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s.,?!\\-_]", " ")
			.replaceAll("\\s{2,}", " ");

		// 정규화된 텍스트와 원본 메타데이터를 유지하여 새 Document 객체 생성
		return new Document(normalizedText, doc.getMetadata());
	}
}
