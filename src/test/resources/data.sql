DELETE FROM crypto_data;
DELETE FROM crypto_currency;
DELETE FROM av_account;

INSERT INTO av_account (user_id, api_key) VALUES ('test_acc_1','AAAAAAAAAAAA'), ('test_acc_2','BBBBBBBBBBB');

INSERT INTO crypto_currency (code, name, av_account, active) VALUES ('CXT1','Crypto Currency Test 1','test_acc_1',1),
('CXT2','Crypto Currency Test 2','test_acc_2',1);

INSERT INTO crypto_data (cx_code, read_time, open, close, high, low) VALUES 
('CXT1','2022-06-30 12:15:00',18357.3239,18399.01359,18399.01359,18334.13731),
('CXT1','2022-06-30 12:00:00',18369.7683,18357.3239,18400.68695,18339.00351),
('CXT1','2022-06-30 11:45:00',18311.00842,18369.77791,18385.7229,18285.1387),
('CXT1','2022-06-30 11:30:00',18362.15163,18311.01804,18454.03245,18307.3732),
('CXT2','2022-06-30 12:15:00',30.38366,30.5374,30.63349,30.29718),
('CXT2','2022-06-30 12:00:00',30.44131,30.37405,30.49897,30.29718),
('CXT2','2022-06-30 11:45:00',30.33561,30.44131,30.47975,30.20109),
('CXT2','2022-06-30 11:30:00',30.28757,30.34522,30.66232,30.26835);