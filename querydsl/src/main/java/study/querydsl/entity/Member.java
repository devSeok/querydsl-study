package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="team_id")
    private Team team;

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;

        if(team != null) {  // 팀 값이 있으면
            changeTeam(team); // 팀을 바꿔준다.
        }
    }

    public Member(String username) {
        this(username, 0, null);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public void changeTeam(Team team) {
            this.team = team;
            team.getMembers().add(this); // 팀에 연관되어 있는 나도 셋팅을 해준다.
    }
}
