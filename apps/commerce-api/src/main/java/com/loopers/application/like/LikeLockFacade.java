package com.loopers.application.like;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LikeLockFacade {

    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final LikeFacade likeFacade;

    public LikeInfo recordLike(LikeCommand.LikeProductCommand command) {
        for (int retryCount = 0; retryCount < MAX_RETRY_ATTEMPTS; retryCount++) {
            try {
                return likeFacade.recordLike(command);
            } catch (OptimisticLockingFailureException e) {
                if (retryCount == MAX_RETRY_ATTEMPTS - 1) {
                    throw new CoreException(ErrorType.CONFLICT, "좋아요 등록 중 동시성 충돌이 발생했습니다. 다시 시도해주세요.");
                }
                try {
                    Thread.sleep(10 * (retryCount + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CoreException(ErrorType.INTERNAL_ERROR, "좋아요 등록 중 오류가 발생했습니다.");
                }
            }
        }
        throw new CoreException(ErrorType.INTERNAL_ERROR, "좋아요 등록에 실패했습니다.");
    }

    public LikeInfo cancelLike(LikeCommand.LikeProductCommand command) {
        for (int retryCount = 0; retryCount < MAX_RETRY_ATTEMPTS; retryCount++) {
            try {
                return likeFacade.cancelLike(command);
            } catch (OptimisticLockingFailureException e) {
                if (retryCount == MAX_RETRY_ATTEMPTS - 1) {
                    throw new CoreException(ErrorType.CONFLICT, "좋아요 취소 중 동시성 충돌이 발생했습니다. 다시 시도해주세요.");
                }
                try {
                    Thread.sleep(10 * (retryCount + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CoreException(ErrorType.INTERNAL_ERROR, "좋아요 취소 중 오류가 발생했습니다.");
                }
            }
        }
        throw new CoreException(ErrorType.INTERNAL_ERROR, "좋아요 취소에 실패했습니다.");
    }
}
