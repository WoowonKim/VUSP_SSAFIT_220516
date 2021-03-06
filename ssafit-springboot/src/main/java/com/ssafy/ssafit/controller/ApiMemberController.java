package com.ssafy.ssafit.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ssafit.exception.PWIncorrectException;
import com.ssafy.ssafit.exception.UserNotFoundException;
import com.ssafy.ssafit.model.dto.Member;
import com.ssafy.ssafit.model.service.MemberService;
import com.ssafy.ssafit.util.JWTUtil;

@RestController
@RequestMapping("/member")
public class ApiMemberController {
	private static final String SUCESS = "sucess";
	private static final String FAIL = "fail";
	private static final String HEADER_AUTH = "auth-token";

	@Autowired
	private MemberService memberService;

	@Autowired
	private JWTUtil jwtUtil;

	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody Member member) {
		HttpStatus status = null;
		HashMap<String, Object> result = new HashMap<>();
		System.out.println(member);
		try {
			// user 정보 DB확인
			if (member.getUserId() != null || member.getUserId().length() > 0) {
				Member m = memberService.getMember(member.getUserId());
				System.out.println(m.getUserId());
				if (m == null || m.getUserId().equals(""))
					throw new UserNotFoundException();
				if (!member.getPassword().equals(m.getPassword()))
					throw new PWIncorrectException();
				result.put("auth-token", jwtUtil.createToken("userId", member.getUserId()));
				result.put("msg", SUCESS);
				result.put("logonMember", m);
				status = HttpStatus.ACCEPTED;
			} else {
				result.put("msg", FAIL);
				status = HttpStatus.UNAUTHORIZED; // 권한없음
			}
		} catch (UserNotFoundException e) {
			result.put("msg", "UserNotFoundException");
			status = HttpStatus.UNAUTHORIZED;
		} catch (PWIncorrectException e) {
			result.put("msg", "PWIncorrectException");
			status = HttpStatus.UNAUTHORIZED;
		} catch (Exception e) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<Map<String, Object>>(result, status);
	}

	@GetMapping("/{userId}")
	public ResponseEntity<Map<String, Object>> getMember(@PathVariable String userId) {
		HttpStatus status = null;
		HashMap<String, Object> result = new HashMap<>();

		try {
			result.put("logonMember", memberService.getMember(userId));
			result.put("followList", memberService.getFollowList(userId));
			result.put("leadList", memberService.getLeadList(userId));
			status = HttpStatus.OK;
		} catch (Exception e) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		return new ResponseEntity<Map<String, Object>>(result, status);
	}

	@GetMapping("/other/{userId}")
	public ResponseEntity<Map<String, Object>> getOtherMember(@PathVariable String userId) {
		HttpStatus status = null;
		HashMap<String, Object> result = new HashMap<>();
		try {
			result.put("followList", memberService.getFollowList(userId));
			result.put("leadList", memberService.getLeadList(userId));
			status = HttpStatus.OK;
		} catch (Exception e) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		return new ResponseEntity<Map<String, Object>>(result, status);
	}

	@PostMapping("/join")
	public ResponseEntity<Member> join(@RequestBody Member member) {
		HttpStatus status = null;
		try {
			memberService.joinMember(member);
			status = HttpStatus.CREATED;
			member = null; // 정보는 제거하고, 로그인 페이지로 이동
		} catch (Exception e) {
			status = HttpStatus.CONFLICT; // 정보를 들고 다시 join으로 이동
		}
		return new ResponseEntity<Member>(member, status);
	}

	@PutMapping("/{userId}")
	public ResponseEntity<Member> update(@PathVariable String userId, @RequestBody Member member,
			HttpServletRequest req) {
		HttpStatus status = null;
		String token = req.getHeader(HEADER_AUTH);
		try {
			if (jwtUtil.getTokenId(token).equals(userId)) {
				System.out.println(member);
				memberService.updateMember(member);
				status = HttpStatus.CREATED; // 수정 완료
			} else
				status = HttpStatus.CONFLICT;
		} catch (Exception e) {
			status = HttpStatus.CONFLICT; //
		}
		return new ResponseEntity<Member>(memberService.getMember(userId), status);
	} // token이 반드시 담겨있어야 함.

	@DeleteMapping("/{userId}")
	public ResponseEntity<String> delete(@PathVariable String userId, HttpServletRequest req) {
		HttpStatus status = null;
		String token = req.getHeader(HEADER_AUTH);
		try {
			if (jwtUtil.getTokenId(token).equals(userId)) {
				memberService.deleteMember(userId);
				status = HttpStatus.NO_CONTENT;
			} else
				status = HttpStatus.CONFLICT;
		} catch (Exception e) {
			status = HttpStatus.CONFLICT;
		}
		return new ResponseEntity<String>("deleted : " + userId, status);
	}

	@PostMapping("/follow/{userId}/{followId}")
	public ResponseEntity<Map<String, Object>> follow(@PathVariable String userId, @PathVariable String followId) {
		HttpStatus status = null;

		HashMap<String, String> params = new HashMap<>();
		params.put("userId", userId);
		params.put("followId", followId);

		HashMap<String, Object> result = new HashMap<>();
		try {
			memberService.followId(params);
			result.put("followList", memberService.getFollowList(userId));
			result.put("leadList", memberService.getLeadList(userId));
			status = HttpStatus.CREATED;
		} catch (Exception e) {
			status = HttpStatus.CONFLICT;
		}

		return new ResponseEntity<Map<String, Object>>(result, status);
	}

	@DeleteMapping("/follow/{userId}/{followId}")
	public ResponseEntity<Map<String, Object>> unfollow(@PathVariable String userId, @PathVariable String followId) {
		HttpStatus status = null;
		System.out.println(userId + " :: " + followId);
		HashMap<String, String> params = new HashMap<>();
		params.put("userId", userId);
		params.put("followId", followId);

		HashMap<String, Object> result = new HashMap<>();
		try {
			memberService.unFollowId(params);
			result.put("followList", memberService.getFollowList(userId));
			result.put("leadList", memberService.getLeadList(userId));
			status = HttpStatus.ACCEPTED;
		} catch (Exception e) {
			status = HttpStatus.CONFLICT;
		}

		return new ResponseEntity<Map<String, Object>>(result, status);
	}

	// id 중복체크 api
	@PostMapping("/join/checkId")
	public ResponseEntity<String> checkId(@RequestBody String userId) {
		String msg = "";
		HttpStatus status = null;
		userId = userId.replaceAll("\"", ""); // 이거 왜?
		Member member = memberService.getMember(userId);
		if (member == null) {
			msg = "ok";
			status = HttpStatus.ACCEPTED;
		} else {
			msg = "no";
			status = HttpStatus.NO_CONTENT;
		}
		return new ResponseEntity<String>(msg, status);
	}

	@PostMapping("/join/checkName")
	public ResponseEntity<String> checkName(@RequestBody String username) {
		String msg = "";
		HttpStatus status = null;
		username = username.replaceAll("\"", ""); // 이거 왜?
		Member member = memberService.getMemberByName(username);
		if (member == null) {
			msg = "ok";
			status = HttpStatus.ACCEPTED;
		} else {
			msg = "no";
			status = HttpStatus.NO_CONTENT;
		}
		return new ResponseEntity<String>(msg, status);
	}
}
