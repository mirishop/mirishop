package com.hh.mirishop.comment.service;

import com.hh.mirishop.comment.dto.CommentRequest;
import com.hh.mirishop.comment.entity.Comment;
import com.hh.mirishop.comment.repository.CommentRepository;
import com.hh.mirishop.common.exception.CommentException;
import com.hh.mirishop.common.exception.ErrorCode;
import com.hh.mirishop.common.exception.MemberException;
import com.hh.mirishop.common.exception.PostException;
import com.hh.mirishop.like.domain.LikeType;
import com.hh.mirishop.like.repository.LikeRepository;
import com.hh.mirishop.member.entity.Member;
import com.hh.mirishop.member.repository.MemberRepository;
import com.hh.mirishop.post.entity.Post;
import com.hh.mirishop.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.SoftDelete;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final LikeRepository likeRepository;

    @Override
    @Transactional
    public Long createCommentOrReply(CommentRequest request, Long memberNumber, Long postId) {
        Post post = findPostById(postId);
        Member member = findMemberByNumber(memberNumber);
        Comment parentComment = null;
        Long parentCommentId = request.getParentCommentId();

        // 부모 댓글이 없으면 null로 포함시키고, 있다면 depth 1만 허용
        if (parentCommentId != null) {
            parentComment = findParentCommentById(parentCommentId);
            // 부모 댓글이 상위 부모를 가지는 경우 에러 처리
            if (parentComment.getParentComment() != null) {
                throw new CommentException(ErrorCode.SUBCOMMENT_NOT_ALLOWED);
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .content(request.getContent())
                .member(member)
                .parentComment(parentComment)
                .isDeleted(false)
                .build();

        commentRepository.save(comment);
        /*
        뉴스피드에 대한 로직 고려
        */
        return comment.getCommentId();
    }

    @Override
    @SoftDelete
    @Transactional
    public void deleteComment(Long commentId, Long currentMemberNumber) {
        Comment comment = findCommentById(commentId);

        checkAuthorizedMember(currentMemberNumber, comment);

        comment.delete(true);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public Long findPostIdByCommentId(Long commentId) {
        return commentRepository.findPostIdByCommentId(commentId)
                .orElseThrow(() -> new CommentException(ErrorCode.POST_NOT_FOUND));
    }

    @Override
    @Transactional
    public List<Long> findCommentIdsByMemberNumber(Long memberNumber) {
        return commentRepository.findCommentIdsByMemberNumber(memberNumber);
    }

    @Transactional
    public Integer countLikeForComment(Long commentId) {
        return likeRepository.countByItemIdAndLikeType(commentId, LikeType.COMMENT);
    }

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostException(ErrorCode.POST_NOT_FOUND));
    }

    private Comment findParentCommentById(Long parentCommentId) {
        return commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new CommentException(ErrorCode.PARENT_COMMENT_NOT_FOUND));
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private Member findMemberByNumber(Long memberNumber) {
        return memberRepository.findById(memberNumber)
                .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void checkAuthorizedMember(Long currentMemberNumber, Comment comment) {
        if (!comment.getMember().getNumber().equals(currentMemberNumber)) {
            throw new CommentException(ErrorCode.UNAUTHORIZED_COMMENT_ACCESS);
        }
    }
}
