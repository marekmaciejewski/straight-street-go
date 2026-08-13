import { useState } from "react";
import { api, ApiError } from "./api";
import type { CarType, ReservationCreateRequest, ReservationResponse } from "./apiTypes";

type Mode = "reserve" | "lookup";
type SubmitAction = Mode | null;

type FleetOption = {
  type: CarType;
  label: string;
  seats: string;
  available: number;
};

const fleetOptions: FleetOption[] = [
  { type: "SEDAN", label: "Sedan", seats: "city and business trips", available: 2 },
  { type: "SUV", label: "SUV", seats: "family and luggage space", available: 2 },
  { type: "VAN", label: "Van", seats: "group travel", available: 1 }
];

function messageFromError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "Unexpected frontend error.";
}

function toDateTimeLocalValue(date: Date): string {
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return offsetDate.toISOString().slice(0, 16);
}

function createInitialPickupValue(): string {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  tomorrow.setHours(10, 0, 0, 0);
  return toDateTimeLocalValue(tomorrow);
}

function parsePickupDateTime(value: string): string | null {
  const parsedDate = new Date(value);
  return Number.isNaN(parsedDate.getTime()) ? null : parsedDate.toISOString();
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
    timeZoneName: "short"
  }).format(new Date(value));
}

function formatCarType(value: CarType): string {
  return fleetOptions.find((option) => option.type === value)?.label ?? value;
}

function DetailItem({ label, value }: Readonly<{ label: string; value: string | number }>) {
  return (
    <div className="detail-item">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function ReservationDetails({
  reservation,
  title
}: Readonly<{ reservation: ReservationResponse | null; title: string }>) {
  if (!reservation) {
    return null;
  }

  return (
    <section className="result-panel" aria-live="polite">
      <div className="result-heading">
        <h3>{title}</h3>
        <span className="status-badge">confirmed</span>
      </div>

      <dl className="detail-grid">
        <DetailItem label="Reservation" value={`#${reservation.reservationId}`} />
        <DetailItem label="Assigned car" value={`Car ${reservation.carId}`} />
        <DetailItem label="Type" value={formatCarType(reservation.carType)} />
        <DetailItem label="Customer" value={reservation.customerId} />
        <DetailItem label="Pickup" value={formatDateTime(reservation.pickupDateTime)} />
        <DetailItem label="Return" value={formatDateTime(reservation.returnDateTime)} />
        <DetailItem label="Available again" value={formatDateTime(reservation.availableAgainDateTime)} />
        <DetailItem label="Days" value={reservation.numberOfDays} />
      </dl>
    </section>
  );
}

export default function App() {
  const [mode, setMode] = useState<Mode>("reserve");
  const [carType, setCarType] = useState<CarType>("SEDAN");
  const [pickupDateTime, setPickupDateTime] = useState(createInitialPickupValue);
  const [numberOfDays, setNumberOfDays] = useState("2");
  const [customerId, setCustomerId] = useState("customer-1");
  const [lookupId, setLookupId] = useState("");
  const [submitAction, setSubmitAction] = useState<SubmitAction>(null);
  const [showColdStartHint, setShowColdStartHint] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [reservation, setReservation] = useState<ReservationResponse | null>(null);

  const isBusy = submitAction !== null;
  const isRenderBackend = api.baseUrl.includes("onrender.com");

  function switchMode(nextMode: Mode) {
    setMode(nextMode);
    setErrorMessage(null);
    setReservation(null);
  }

  function showError(message: string) {
    setReservation(null);
    setErrorMessage(message);
  }

  async function runBackendAction(action: Mode, callback: () => Promise<void>) {
    setSubmitAction(action);
    setShowColdStartHint(false);
    setErrorMessage(null);

    const coldStartTimer = globalThis.setTimeout(() => {
      setShowColdStartHint(true);
    }, 7000);

    try {
      await callback();
    } catch (error) {
      showError(messageFromError(error));
    } finally {
      globalThis.clearTimeout(coldStartTimer);
      setSubmitAction(null);
    }
  }

  async function handleReserve() {
    const trimmedCustomerId = customerId.trim();
    const parsedNumberOfDays = Number(numberOfDays);
    const pickupIso = parsePickupDateTime(pickupDateTime);

    if (!pickupIso) {
      showError("Choose a valid pickup date and time.");
      return;
    }

    if (!Number.isInteger(parsedNumberOfDays) || parsedNumberOfDays < 1) {
      showError("Days must be a whole number greater than zero.");
      return;
    }

    if (!trimmedCustomerId || trimmedCustomerId.length > 128) {
      showError("Enter a customer id up to 128 characters.");
      return;
    }

    const command: ReservationCreateRequest = {
      carType,
      pickupDateTime: pickupIso,
      numberOfDays: parsedNumberOfDays,
      customerId: trimmedCustomerId
    };

    await runBackendAction("reserve", async () => {
      const createdReservation = await api.createReservation(command);
      setReservation(createdReservation);
      setLookupId(String(createdReservation.reservationId));
      setCustomerId(createdReservation.customerId);
    });
  }

  async function handleLookup() {
    const parsedReservationId = Number(lookupId);

    if (!Number.isInteger(parsedReservationId) || parsedReservationId < 1) {
      showError("Enter a reservation id greater than zero.");
      return;
    }

    await runBackendAction("lookup", async () => {
      const foundReservation = await api.getReservation(parsedReservationId);
      setReservation(foundReservation);
      setCarType(foundReservation.carType);
      setCustomerId(foundReservation.customerId);
    });
  }

  function renderReserveForm() {
    return (
      <form
        className="action-form"
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          void handleReserve();
        }}
      >
        <fieldset className="fleet-picker">
          <legend>Vehicle type</legend>
          <div className="fleet-segments" role="radiogroup" aria-label="Vehicle type">
            {fleetOptions.map((option) => (
              <button
                key={option.type}
                className={`fleet-segment ${carType === option.type ? "active" : ""}`}
                type="button"
                role="radio"
                aria-checked={carType === option.type}
                onClick={() => setCarType(option.type)}
              >
                <span className="fleet-segment-label">{option.label}</span>
                <span>{option.available} available</span>
              </button>
            ))}
          </div>
        </fieldset>

        <div className="form-grid">
          <div>
            <label className="form-label" htmlFor="pickup-date-time">
              Pickup
            </label>
            <input
              id="pickup-date-time"
              className="form-control app-input"
              type="datetime-local"
              value={pickupDateTime}
              onChange={(event) => setPickupDateTime(event.target.value)}
            />
          </div>
          <div>
            <label className="form-label" htmlFor="number-of-days">
              Days
            </label>
            <input
              id="number-of-days"
              className="form-control app-input"
              type="number"
              min={1}
              step={1}
              value={numberOfDays}
              onChange={(event) => setNumberOfDays(event.target.value)}
            />
          </div>
        </div>

        <div>
          <label className="form-label" htmlFor="customer-id">
            Customer id
          </label>
          <input
            id="customer-id"
            className="form-control app-input"
            type="text"
            value={customerId}
            maxLength={128}
            autoComplete="off"
            onChange={(event) => setCustomerId(event.target.value)}
          />
        </div>

        <div className="action-row">
          <button className="btn primary-action" type="submit" disabled={isBusy}>
            {submitAction === "reserve" ? "Reserving..." : "Reserve car"}
          </button>
        </div>
      </form>
    );
  }

  function renderLookupForm() {
    return (
      <form
        className="action-form"
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          void handleLookup();
        }}
      >
        <div>
          <label className="form-label" htmlFor="reservation-id">
            Reservation id
          </label>
          <input
            id="reservation-id"
            className="form-control app-input reservation-id-input"
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            value={lookupId}
            placeholder="101"
            onChange={(event) => setLookupId(event.target.value.replace(/\D/g, ""))}
          />
        </div>

        <div className="action-row">
          <button className="btn primary-action" type="submit" disabled={isBusy}>
            {submitAction === "lookup" ? "Loading..." : "Find reservation"}
          </button>
        </div>
      </form>
    );
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="utility-bar">
          <div className="container page-container utility-content">
            <span className="utility-spacer" />
            <a href={`${api.baseUrl}/v3/api-docs`} target="_blank" rel="noreferrer">
              OpenAPI
            </a>
            <a href={`${api.baseUrl}/swagger-ui/index.html`} target="_blank" rel="noreferrer">
              Swagger UI
            </a>
          </div>
        </div>

        <nav className="brand-bar" aria-label="Main navigation">
          <div className="container page-container brand-content">
            <a className="brand-link" href={import.meta.env.BASE_URL} aria-label="Straight Street Go">
              <img src={`${import.meta.env.BASE_URL}tread-mark.svg`} alt="" className="brand-mark" />
              <span>
                <span className="brand-name">Straight Street</span>
                <span className="brand-name">Go</span>
              </span>
            </a>
          </div>
        </nav>

        <div className="section-nav">
          <div className="container page-container section-nav-content">
            <span className="section-nav-link active">Reservations</span>
          </div>
        </div>
      </header>

      <main>
        <section className="hero-stage">
          <img
            className="hero-image"
            src={`${import.meta.env.BASE_URL}fleet-hero.png`}
            alt=""
            aria-hidden="true"
          />
          <div className="container page-container hero-content">
            <section className="reservation-panel" aria-labelledby="reservation-title">
              <div className="mode-tabs" role="tablist" aria-label="Reservation actions">
                <button
                  className={`mode-tab ${mode === "reserve" ? "active" : ""}`}
                  type="button"
                  role="tab"
                  aria-selected={mode === "reserve"}
                  onClick={() => switchMode("reserve")}
                >
                  Reserve
                </button>
                <button
                  className={`mode-tab ${mode === "lookup" ? "active" : ""}`}
                  type="button"
                  role="tab"
                  aria-selected={mode === "lookup"}
                  onClick={() => switchMode("lookup")}
                >
                  Find
                </button>
              </div>

              <h1 id="reservation-title">Reserve the first available car by type.</h1>
              <p className="panel-intro">
                Choose the vehicle class, pickup time, and trip length. Straight Street Go will confirm the best
                matching car from the available fleet.
              </p>

              {showColdStartHint && isRenderBackend && (
                <output className="alert alert-warning status-alert">
                  Render is waking the service after idle time. The first response can take about a minute.
                </output>
              )}

              {errorMessage && (
                <div className="alert alert-danger status-alert" role="alert">
                  {errorMessage}
                </div>
              )}

              {mode === "reserve" ? renderReserveForm() : renderLookupForm()}
              <ReservationDetails
                reservation={reservation}
                title={mode === "reserve" ? "Latest reservation" : "Reservation details"}
              />
            </section>
          </div>
        </section>
      </main>

      <footer className="api-footer">
        <div className="container page-container footer-content">
          <span className="api-badge">{isRenderBackend ? "Render" : "Local"}</span>
          <span>{api.baseUrl}</span>
        </div>
      </footer>
    </div>
  );
}
