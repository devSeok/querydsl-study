package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private MemberJpaRepository memberJpaRepository;


    @Test
    public void 저장() {
        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);

        Member findMember = memberJpaRepository.findById(member1.getId()).get();
        Assertions.assertThat(findMember).isEqualTo(member1);

        List<Member> result = memberJpaRepository.findAll_querydsl();
        Assertions.assertThat(result).containsExactly(member1);

        List<Member> findName = memberJpaRepository.findByName_querydsl("member1");
        Assertions.assertThat(findName).containsExactly(member1);

    }
}