# 🏆 Viễn Thông Cup 2026 - Tournament Management System

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-F2F4F9?style=for-the-badge&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-323330?style=for-the-badge&logo=javascript&logoColor=F7DF1E)
![Bootstrap](https://img.shields.io/badge/Bootstrap-563D7C?style=for-the-badge&logo=bootstrap&logoColor=white)

Hệ thống quản lý giải đấu bóng đá tự động hóa toàn diện, được thiết kế với kiến trúc **API-First** và tư duy xử lý dữ liệu chuyên sâu (Data Engineering). Dự án giải quyết các bài toán thực tế trong tổ chức giải đấu phong trào: xung đột tài nguyên xếp lịch, sai số tính toán và bất công trong thể thức lệch bảng.

---

## 🚀 Core Logic & Tính năng nổi bật

Dự án không chỉ dừng lại ở các thao tác CRUD cơ bản mà tập trung vào việc xây dựng các thuật toán xử lý nghiệp vụ (Service Layer) mạnh mẽ:

- **Auto-Scheduling (Round-Robin & Constraint Satisfaction):**  
  Tự động sinh lịch thi đấu vòng tròn dựa trên Hàng đợi (Queue).  
  Sử dụng cấu trúc `HashSet` ($O(1)$) để kiểm tra xung đột thời gian thực, đảm bảo không có đội bóng nào thi đấu 2 trận/ngày.

- **Data Normalization (PPG - Points Per Game):**  
  Giải quyết bài toán chênh lệch số trận giữa bảng 4 và 5 đội.  
  Tính toán PPG, GDPG để đảm bảo tính công bằng.

- **Global Seeding & Cross-Pairing:**  
  Chuyển từ Group Stage sang Knockout Bracket.  
  Seed $i$ gặp Seed $N-1-i$ để cân bằng sức mạnh.

- **Frontend Concurrency & Real-time Update:**  
  CSR + `Promise.all` để gửi batch request, tối ưu hiệu năng.

---

## 💻 Tech Stack

- **Backend:** Java 17, Spring Boot, Spring Data JPA, Hibernate  
- **Database:** PostgreSQL  
- **Frontend:** HTML5, JavaScript (ES6+), CSS3, Bootstrap 5  
- **Architecture:** RESTful API, Client-Side Rendering (CSR)

---

## ⚙️ Hướng dẫn cài đặt (Local Development)

### 1. Yêu cầu hệ thống

- Java JDK 17+
- PostgreSQL
- IntelliJ IDEA (Backend)
- VS Code (Frontend)

---

### 2. Thiết lập Database

Tạo database:

```sql
CREATE DATABASE vienthong_cup;
````

Cấu hình trong `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/vienthong_cup
spring.datasource.username=postgres
spring.datasource.password=Thienanh1906@
spring.jpa.hibernate.ddl-auto=update
```

---

### 3. Khởi động Backend

* Mở project bằng IntelliJ
* Chạy `DemoApplication.java`

Server chạy tại:

```
http://localhost:8082
```

---

### 4. Khởi động Frontend

* Mở folder frontend bằng VS Code
* Cài **Live Server**
* Chuột phải `index.html` → Open with Live Server

---

## 📂 Cấu trúc dự án

```plaintext
📦 vienthong-cup-2026
 ┣ 📂 backend
 ┃ ┣ 📂 src/main/java/com/example/demo
 ┃ ┃ ┣ 📂 controller
 ┃ ┃ ┣ 📂 model
 ┃ ┃ ┣ 📂 repository
 ┃ ┃ ┗ 📂 service
 ┃ ┗ 📜 application.properties
 ┣ 📂 frontend
 ┃ ┣ 📜 index.html
 ┃ ┣ 📜 setup.html
 ┃ ┣ 📜 schedule.html
 ┃ ┣ 📜 bracket.html
 ┃ ┗ 📜 players.html
 ┗ 📜 README.md
```

---

## 👨‍💻 Tác giả

**Nguyễn Hoàng Thiện Anh**

* Sinh viên Data Engineering - PTIT
* Bí thư Liên chi đoàn Khoa Viễn Thông 1

---

⭐ Nếu bạn thấy dự án hữu ích, hãy cho mình một star nhé!

```

---

```
