package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QusertdslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamB);

        em.persist(member1);
        em.persist(member2);
    }

    @Test
    public void startJPQL() {
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();


        assertThat(findMember.getUsername()).isEqualTo("member12");
    }

    @Test
    public void startQuerydsl() {

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10, 20)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

//        Member fetchOne = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst();
//
//        // featchResult() 권장하지 않음 그래서 count 는 이렇게 처리해야됨.
//        Long totalCount = queryFactory
//                .select(member.count())
//                .from(member)
//                .fetchOne();


//        System.out.println(fetch);
    }


    /**
     * 회원 정렬
     * 1. 회원 나이 내림차순 ( desc)
     * 2. 회원 나이 올림차순 (asc)
     */
    @Test
    public void sort() {

        em.persist(new Member(null, 100));
        em.persist(new Member("member3", 100));
        em.persist(new Member("member4", 100));

        List<Member> fetch = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member3 = fetch.get(0);
        Member member4 = fetch.get(1);
        Member memberNull = fetch.get(2);

        assertThat(member3.getUsername()).isEqualTo("member3");
        assertThat(member4.getUsername()).isEqualTo("member4");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        System.out.println(fetch.size());
        for (Member member : fetch) {
            System.out.println(member);
        }

        assertThat(fetch.size()).isEqualTo(1);
    }

    @Test
    public void aggregation() {
        List<Tuple> list = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = list.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(2);
    }

    /**
     * 팀 이름과 각 팀 평균 연령을 구해라
     *
     * @throws Exception
     */
    @Test
    public void group() throws Exception {
        List<Tuple> fetch = queryFactory
                .select(team, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .having(team.name.eq("teamA"))
                .fetch();

        for (Tuple t : fetch) {
            System.out.println(t);
        }
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> teamA = queryFactory.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();


        assertThat(teamA)
                .extracting("username")
                .containsExactly("member2");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인 , 회원은 모두 조회
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> teamA = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : teamA) {
            System.out.println(tuple);
        }
    }

    /**
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        queryFactory.select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
    }



    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // 로딩된 엔티티인지 초기화 안됨 엔티티인지
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

       assertThat(loaded).as("페치 조인 미적용").isFalse();
        System.out.println(loaded);
    }


    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();


        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isTrue();
        System.out.println(loaded);
    }

    /**
     *  나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = queryFactory.selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(fetch).extracting("age")
                .containsExactly(20);

    }


    @Test
    public void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = queryFactory.selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(fetch).extracting("age")
                .containsExactly(20);
    }

    @Test
    public void basicCase() {
        List<String> reult = queryFactory
                .select(
                        new CaseBuilder()
                                .when(member.age.between(0, 10)).then("0~ 20살")
                                .otherwise("기타"))
                .from(member)
                .fetch();

        System.out.println(reult);
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member, Expressions.constant("A"))
                .from(member)
                .fetch();

        System.out.println(result);
    }

    @Test
    public void concat() {
        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .where(member.username.eq("member1"))
                .from(member)
                .fetch();

        for(String s : fetch) {
            System.out.println(s);
        }
    }


    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> fetch = queryFactory
                .select(member.username, member.username)
                .from(member)
                .fetch();

        for (Tuple s : fetch) {
            System.out.println(s.get(member.username));
        }
    }

    @Test
    public void findDtoByJPQL() {

        List<String> resultList = em.createQuery("select m.username from Member m", String.class)
                .getResultList();

        System.out.println(resultList);
    }

    @Test
    public void findDtoBySetter() {
        // 프로퍼티 접근 방법 set / get
        List<MemberDto> fetch = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findDtoByField() {
        // 프로퍼티 접근 방법
        List<MemberDto> fetch = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println(memberDto);
        }
    }


    @Test
    public void findDtoByConstructor() {
        // 프로퍼티 접근 방법
        List<MemberDto> fetch = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findUserDtoByField() {
        List<UserDto> f = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),  member.age))
                .from(member)
                .fetch();
    }
}
