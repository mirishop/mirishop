package com.hh.mirishop.like.repository;

import com.hh.mirishop.like.domain.LikeType;
import com.hh.mirishop.like.entity.Like;
import com.hh.mirishop.member.entity.Member;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // N+1 문제 해결을 위한 Fetch Join 사용
    // Fetch Join을 사용하여 특정 아이템 타입과 관련된 모든 좋아요 조회
    @Query("SELECT l FROM Like l JOIN FETCH l.member WHERE l.itemId = :itemId AND l.likeType = :likeType")
    List<Like> findAllByItemIdAndLikeTypeWithFetchJoin(@Param("itemId") Long itemId, @Param("likeType") LikeType likeType);


    // itemId(post 또는 comment)에서 member가 좋아요를 했는지 확인하는 메소드
    boolean existsByItemIdAndLikeTypeAndMember(Long itemId, LikeType likeType, Member member);

    Optional<Like> findByItemIdAndLikeTypeAndMember(Long itemId, LikeType likeType, Member member);
}
