package com.smarthealthdog.backend.domain;

//import해서 필요한 라이브러리 불러오기(jpa, 반복되는 개터,생성자)
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;  //생년월일용

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pets")

public class Pet { 
    //기본키
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //반려동물 이름, 필수(not null),최대 30자
    @Column(nullable=false, length=30)
    private String name;

    //종-enum타입, @enumerated 이거는 db에서 숫자가 아닌 문자열로 저장해줌
    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    private Species species;

    //품종,선택 입력(nullable 허용)
    @Column(length=50)
    private String breed;

    //성별(male/female/unknown), 기본값을 unknown으로 설정
    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    private  Sex sex = Sex.UNKNOWN;

    //생일(날짜,nullable 가능)
    private LocalDate birthDate;

    //중성화 여부, 기본값 false
    @Column(nullable=false)
    private Boolean neutered = false;

    //몸무게, 선택입력
    private Double weightKg;

    //반려동물 주인의 id(user엔티티와 연동할 수 있는 부분)
    @Column(nullable=false)
    private Long ownerId;

    @Builder
    //생성자&빌더: 객체를 편하게 생성 가능, sex나 neutered가 null이면 기본값 넣도록 방어코드
    private Pet(String name, Species species, String breed, Sex sex, 
                LocalDate birthDate, Boolean neutered, Double weightKg, 
                Long ownerId){
                    this.name = name;
                    this.species = species;
                    this.breed = breed;
                    this.sex = sex != null ? sex : Sex.UNKNOWN;
                    this.birthDate = birthDate;
                    this.neutered = neutered != null ? neutered : false;
                    this.weightKg = weightKg;
                    this.ownerId = ownerId;
                }
    //수정메서드: 나중에 수정api(pull)호출하면 이 메서드 사용해서 필드 값 갱신, 역시 null 들어오면 기본값으로 처리
    public void update(String name, Species species, String breed, Sex sex, 
                LocalDate birthDate, Boolean neutered, Double weightKg){
                    this.name = name;
                    this.species = species;
                    this.breed = breed;
                    this.sex = sex != null ? sex : Sex.UNKNOWN;
                    this.birthDate = birthDate;
                    this.neutered = neutered != null ? neutered : false;
                    this.weightKg = weightKg;
                }
}
