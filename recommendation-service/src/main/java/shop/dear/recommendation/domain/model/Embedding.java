package shop.dear.recommendation.domain.model;

import java.util.Arrays;

//임베딩 결과
public record Embedding(
	String modelName,
	float[] values
) {

	public Embedding {
		if (modelName.isBlank()) {
			throw new IllegalArgumentException("모델명이 없습니다");
		}
		if (values.length == 0) {
			throw new IllegalArgumentException("빈 벡터입니다");
		}
		values = values.clone();
	}

	//벡터 위치
	@Override
	public float[] values() {
		return this.values.clone();
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof Embedding other
			&& this.modelName.equals(other.modelName)
			&& Arrays.equals(this.values, other.values);
	}

	@Override
	public int hashCode() {
		return 31 * this.modelName.hashCode() + Arrays.hashCode(this.values);
	}
}
