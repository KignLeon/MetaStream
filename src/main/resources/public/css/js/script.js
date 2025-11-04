// basic UI behaviors: year fill, nav toggle, header hide on scroll, simple form success modal
document.addEventListener('DOMContentLoaded', () => {
  // year placeholders
  const years = document.querySelectorAll('#year,#year2,#year3,#year4,#year5');
  years.forEach(el => { if(el) el.textContent = new Date().getFullYear(); });

  // nav toggle for small screens
  const navToggle = document.getElementById('nav-toggle');
  const nav = document.querySelector('.nav');
  navToggle && navToggle.addEventListener('click', () => { nav && nav.classList.toggle('active'); });

  // header hide on scroll (peek-a-boo)
  let lastScroll = 0;
  const header = document.getElementById('main-header');
  window.addEventListener('scroll', () => {
    const st = window.scrollY || window.pageYOffset;
    if (st > lastScroll && st > 120) { header && header.classList.add('header-hidden'); }
    else { header && header.classList.remove('header-hidden'); }
    lastScroll = st <= 0 ? 0 : st;
  });

  // enroll form handler (demo only)
  const enrollForm = document.getElementById('enrollForm') || document.getElementById('enroll-form');
  if (enrollForm) {
    enrollForm.addEventListener('submit', (e) => {
      e.preventDefault();
      // show simple success modal / toast
      alert('Thanks! Your free session request has been submitted. We will contact you soon.');
      enrollForm.reset();
    });
  }

  // stripe buttons placeholder
  document.querySelectorAll('.stripe-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      alert('Stripe checkout placeholder — integrate Stripe Checkout or Elements here.');
    });
  });
});