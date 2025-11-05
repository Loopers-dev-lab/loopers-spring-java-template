package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Email {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");

	private String value;

	public Email(String value) {
		validate(value);
		this.value = value;
	}

	private void validate(String value) {
		if (value == null || value.isBlank()) {
			throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
		}
		if (!EMAIL_PATTERN.matcher(value).matches()) {
			throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 xx@yy.zz 형식이어야 합니다.");
		}
	}
}
