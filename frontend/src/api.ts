import type { ProblemDetail, ReservationCreateRequest, ReservationResponse } from "./apiTypes";

const fallbackApiBaseUrl = "http://localhost:8080";
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || fallbackApiBaseUrl).replace(/\/$/, "");

export class ApiError extends Error {
  readonly status?: number;
  readonly problem?: ProblemDetail;

  constructor(message: string, status?: number, problem?: ProblemDetail) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.problem = problem;
  }
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.title === "string" &&
    typeof candidate.status === "number" &&
    typeof candidate.detail === "string" &&
    typeof candidate.instance === "string"
  );
}

async function readResponseBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get("content-type") ?? "";

  if (contentType.includes("application/json") || contentType.includes("application/problem+json")) {
    return response.json();
  }

  const text = await response.text();
  return text.length > 0 ? text : undefined;
}

const fieldLabels: Record<string, string> = {
  carType: "vehicle type",
  customerId: "customer id",
  numberOfDays: "days",
  pickupDateTime: "pickup time",
  reservationId: "reservation id"
};

function fieldLabel(field: string): string {
  return fieldLabels[field] ?? field.replace(/([A-Z])/g, " $1").toLowerCase();
}

function requiredFieldMessage(field: string): string {
  switch (field) {
    case "carType":
      return "Choose a vehicle type.";
    case "pickupDateTime":
      return "Choose a pickup date and time.";
    case "numberOfDays":
      return "Enter the number of days.";
    case "customerId":
      return "Enter a customer id.";
    default:
      return `Enter ${fieldLabel(field)}.`;
  }
}

function formatDateTime(value: string): string | null {
  const parsedDate = new Date(value);

  if (Number.isNaN(parsedDate.getTime())) {
    return null;
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
    timeZoneName: "short"
  }).format(parsedDate);
}

function formatFieldError(field: string, message: string): string {
  const normalizedMessage = message.toLowerCase();

  if (normalizedMessage.includes("must not be blank") || normalizedMessage.includes("must not be null")) {
    return requiredFieldMessage(field);
  }

  if (field === "numberOfDays" && normalizedMessage.includes("greater than or equal to 1")) {
    return "Days must be at least 1.";
  }

  if (field === "customerId" && normalizedMessage.includes("size must be between")) {
    return "Customer id must be 128 characters or fewer.";
  }

  return `${fieldLabel(field)} ${message}.`;
}

function formatValidationProblem(problem: ProblemDetail): string {
  const fieldErrors = problem.errors
    ?.map((error) => formatFieldError(error.field, error.message))
    .join(" ");

  return fieldErrors ? `Please check the form. ${fieldErrors}` : "Please check the form and try again.";
}

function formatConflictProblem(problem: ProblemDetail): string {
  const nextPickup = /Next available pickup date is (?<dateTime>.+)$/u.exec(problem.detail)?.groups?.dateTime;
  const formattedNextPickup = nextPickup ? formatDateTime(nextPickup) : null;

  if (formattedNextPickup) {
    return `That vehicle type is not available for those dates. The next available pickup is ${formattedNextPickup}.`;
  }

  return "That vehicle type is not available for those dates. Try a different pickup time or vehicle type.";
}

function formatProblem(problem: ProblemDetail): string {
  switch (problem.status) {
    case 400:
      return formatValidationProblem(problem);
    case 404:
      return "We couldn't find that reservation. Check the id and try again.";
    case 409:
      return formatConflictProblem(problem);
    default:
      return "The reservation service couldn't complete that request. Please try again.";
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;

  try {
    const headers = new Headers(init.headers);
    headers.set("Accept", "application/json");

    response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      headers
    });
  } catch {
    const wakeUpHint = apiBaseUrl.includes("onrender.com") ? " The hosted service may need a minute to wake up." : "";
    throw new ApiError(
      `We couldn't reach the reservation service. Make sure it is running, then try again.${wakeUpHint}`
    );
  }

  const body = await readResponseBody(response);

  if (!response.ok) {
    if (isProblemDetail(body)) {
      throw new ApiError(formatProblem(body), response.status, body);
    }

    throw new ApiError("The reservation service couldn't complete that request. Please try again.", response.status);
  }

  return body as T;
}

function jsonRequest<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
}

export const api = {
  baseUrl: apiBaseUrl,
  createReservation: (command: ReservationCreateRequest) =>
    jsonRequest<ReservationResponse>("/reservations", command),
  getReservation: (reservationId: number) =>
    request<ReservationResponse>(`/reservations/${encodeURIComponent(reservationId)}`)
};
