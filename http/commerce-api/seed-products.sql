-- 시드 데이터 생성 스크립트: 브랜드 20개 + 상품 10만건
-- 사용법: MySQL 클라이언트에서 직접 실행

-- 1. 브랜드 20개 생성
INSERT INTO brand (name, description, created_at, updated_at) VALUES
('나이키', '스포츠 브랜드', NOW(), NOW()),
('아디다스', '스포츠 브랜드', NOW(), NOW()),
('뉴발란스', '러닝 브랜드', NOW(), NOW()),
('푸마', '스포츠 브랜드', NOW(), NOW()),
('리복', '피트니스 브랜드', NOW(), NOW()),
('컨버스', '캐주얼 브랜드', NOW(), NOW()),
('반스', '스케이트 브랜드', NOW(), NOW()),
('노스페이스', '아웃도어 브랜드', NOW(), NOW()),
('파타고니아', '아웃도어 브랜드', NOW(), NOW()),
('유니클로', '캐주얼 브랜드', NOW(), NOW()),
('자라', '패스트패션 브랜드', NOW(), NOW()),
('H&M', '패스트패션 브랜드', NOW(), NOW()),
('폴로', '프리미엄 캐주얼', NOW(), NOW()),
('타미힐피거', '프리미엄 캐주얼', NOW(), NOW()),
('캘빈클라인', '프리미엄 브랜드', NOW(), NOW()),
('구찌', '럭셔리 브랜드', NOW(), NOW()),
('프라다', '럭셔리 브랜드', NOW(), NOW()),
('버버리', '럭셔리 브랜드', NOW(), NOW()),
('디올', '럭셔리 브랜드', NOW(), NOW()),
('루이비통', '럭셔리 브랜드', NOW(), NOW());

-- 2. 상품 10만건 생성 (프로시저 사용)
DROP PROCEDURE IF EXISTS seed_products;

DELIMITER //
CREATE PROCEDURE seed_products()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE brand_id INT;
    DECLARE product_price BIGINT;
    DECLARE product_stock INT;
    DECLARE like_count INT;

    WHILE i <= 100000 DO
        SET brand_id = (i % 20) + 1;
        SET product_price = 10000 + (FLOOR(RAND() * 490000));
        SET product_stock = 10 + (FLOOR(RAND() * 990));
        SET like_count = FLOOR(RAND() * 1000);

        INSERT INTO product (name, price, stock, brand_id, like_count, created_at, updated_at)
        VALUES (
            CONCAT('상품-', i),
            product_price,
            product_stock,
            brand_id,
            like_count,
            DATE_ADD(NOW(), INTERVAL -FLOOR(RAND() * 365) DAY),
            NOW()
        );

        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

CALL seed_products();
DROP PROCEDURE IF EXISTS seed_products;
