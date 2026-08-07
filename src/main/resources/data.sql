-- 초기 상품 데이터
INSERT INTO product (product_name, product_price) VALUES
('무선마우스',        15000),
('블루투스키보드',    29000),
('USB허브',          39000),
('노트북거치대',      24000),
('웹캠',             45000),
('모니터암',          89000),
('기계식키보드',     129000),
('노이즈캔슬링헤드셋', 259000),
('27인치모니터',     319000),
('외장SSD 1TB',     139000);

-- 초기 고객 (비밀번호는 BCrypt 해시, 원문은 모두 pw1234)
INSERT INTO customer (customer_id, customer_password, customer_point, version) VALUES
('skala01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1000000, 0),
('skala02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 2000000, 0);
