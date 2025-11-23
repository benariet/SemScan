-- Create Session 47 for testing attendance records
-- Run this BEFORE running session-47-attendance-inserts.sql

-- First, check if you have a course_id. If not, you may need to create one or use an existing one.
-- To find existing courses, run: SELECT course_id FROM courses LIMIT 10;

-- Option 1: If you know a course_id, replace <course_id> below with that value
-- INSERT INTO sessions (session_id, course_id, start_time, end_time, status) 
-- VALUES (47, <course_id>, '2025-11-16 14:00:00', '2025-11-16 15:00:00', 'CLOSED')
-- ON DUPLICATE KEY UPDATE session_id = session_id;

-- Option 2: If you don't have a course_id, you can create a test course first:
-- INSERT INTO courses (course_id, course_name, course_code, lecturer_id) 
-- VALUES (1, 'Test Course', 'TEST001', <lecturer_user_id>)
-- ON DUPLICATE KEY UPDATE course_id = course_id;

-- Option 3: Use an existing session_id instead of 47
-- To find existing sessions: SELECT session_id FROM sessions LIMIT 10;
-- Then replace 47 with that session_id in the attendance insert file

-- Example: Create session 47 with a dummy course_id (replace 1 with your actual course_id)
-- INSERT INTO sessions (session_id, course_id, start_time, end_time, status) 
-- VALUES (47, 1, '2025-11-16 14:00:00', '2025-11-16 15:00:00', 'CLOSED')
-- ON DUPLICATE KEY UPDATE session_id = session_id;

